package es.bytescolab.msmaintenance.service.impl;

import es.bytescolab.msmaintenance.client.VehicleClient;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusChangeRequest;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusSnapshot;
import es.bytescolab.msmaintenance.client.vehicle.enums.VehicleStatus;
import es.bytescolab.msmaintenance.dto.request.CompleteOrderRequest;
import es.bytescolab.msmaintenance.dto.request.MaintenanceStatsRequest;
import es.bytescolab.msmaintenance.dto.request.SchedulePlanRequest;
import es.bytescolab.msmaintenance.dto.response.*;
import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import es.bytescolab.msmaintenance.exception.*;
import es.bytescolab.msmaintenance.mapper.MaintenanceMapper;
import es.bytescolab.msmaintenance.repository.MaintenanceOrderRepository;
import es.bytescolab.msmaintenance.repository.MaintenancePlanRespository;
import es.bytescolab.msmaintenance.service.MaintenanceService;
import es.bytescolab.msmaintenance.specification.MaintenanceOrderSpecification;
import es.bytescolab.msmaintenance.specification.MaintenancePlanSpecification;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {


    private final MaintenancePlanRespository maintenancePlanRespository;
    private final MaintenanceOrderRepository maintenanceOrderRepository;
    private final MaintenanceMapper maintenanceMapper;
    private final VehicleClient vehicleClient;

    @Override
    @Transactional
    public PlanDetailsResponse createPlan(SchedulePlanRequest request) {
        final StatusSnapshot vehicle;
        try {
            vehicle = vehicleClient.getVehicle(request.vehicleId());
        } catch (FeignException.NotFound e) {
            throw new VehicleNotFound("No existe un vehículo con el ID proporcionado");
        }

        if (maintenancePlanRespository.existsByVehicleIdAndTypeAndActiveTrue(
                request.vehicleId(), request.type())) {
            throw new PlanAreadyExists(
                    "El vehículo ya tiene un plan activo de este tipo de mantenimiento");
        }

        LocalDate lastDoneAt = request.lastDoneAt() != null
                ? request.lastDoneAt()
                : LocalDate.now(ZoneOffset.UTC);

        Integer lastDoneKm = request.lastDoneKm() != null
                ? request.lastDoneKm()
                : vehicle.odometerKm();

        LocalDate nextDueAt = request.intervalDays() != null
                ? lastDoneAt.plusDays(request.intervalDays())
                : null;

        Integer nextDueKm = request.intervalKm() != null
                ? lastDoneKm + request.intervalKm()
                : null;

        MaintenancePlan toSave = maintenanceMapper.toMaintenancePlan(request);
        toSave.setLastDoneAt(lastDoneAt);
        toSave.setLastDoneKm(lastDoneKm);
        toSave.setNextDueAt(nextDueAt);
        toSave.setNextDueKm(nextDueKm);
        toSave.setActive(true);

        MaintenancePlan saved = maintenancePlanRespository.save(toSave);
        return maintenanceMapper.toPlanDetailsResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanDetailsResponse> listPlans(UUID vehicleId, MaintenanceType maintenanceType, Boolean active) {
        Specification<MaintenancePlan> spec = Specification
                .where(MaintenancePlanSpecification.hasVehicle(vehicleId))
                .and(MaintenancePlanSpecification.hasType(maintenanceType))
                .and(MaintenancePlanSpecification.hasActive(active));
        return maintenancePlanRespository.findAll(spec).stream()
                .map(maintenanceMapper::toPlanDetailsResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> findOrders(
            UUID vehicleId, MaintenanceOrderStatus status, MaintenanceType type,
            LocalDate dueBefore, int page, int size
    ) {
        Specification<MaintenanceOrder> spec = Specification
                .where(MaintenanceOrderSpecification.hasVehicle(vehicleId))
                .and(MaintenanceOrderSpecification.hasStatus(status))
                .and(MaintenanceOrderSpecification.hasType(type))
                .and(MaintenanceOrderSpecification.dueBeforeOrOn(dueBefore));
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "scheduledFor"));
        Page<MaintenanceOrder> orders = maintenanceOrderRepository.findAll(spec, pageable);

        List<OrderSummaryResponse> content = orders
                .map(maintenanceMapper::toOrderSummaryResponse)
                .stream().toList();

        return new PageResponse<>(
                content,
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages());
    }

    @Override
    @Transactional
    public CompletedOrderResponse completeOrder(UUID orderId, CompleteOrderRequest request) {
        MaintenanceOrder orderToUpdate = maintenanceOrderRepository.findById(orderId).orElseThrow(
                () -> new OrderNotFound("No existe una orden con el ID proporcionado")
        );

        if (orderToUpdate.getStatus() != MaintenanceOrderStatus.IN_PROGRESS) {
            throw new InvalidOrderState("Solo se puede completar una orden en curso");
        }

        final StatusSnapshot vehicle;
        try {
            vehicle = vehicleClient.getVehicle(orderToUpdate.getVehicleId());
        } catch (FeignException.NotFound e) {
            throw new VehicleNotFound("No existe un vehículo con el ID proporcionado");
        } catch (FeignException.ServiceUnavailable | RetryableException e) {
            throw new ServiceUnavailableException("ms-vehicles no responde", e);
        }

        int finalOdometer = request.odometerKm() != null
                ? request.odometerKm()
                : vehicle.odometerKm();
        if (vehicle.odometerKm() != null && finalOdometer < vehicle.odometerKm()) {
            throw new InvalidOdometerException("El odómetro no puede ser menor que el actual del vehículo");
        }

        try {
            vehicleClient.updateVehicle(
                    orderToUpdate.getVehicleId(),
                    new StatusChangeRequest(VehicleStatus.AVAILABLE, finalOdometer));
        } catch (FeignException.ServiceUnavailable | RetryableException e) {
            throw new ServiceUnavailableException("ms-vehicles no responde", e);
        }

        Instant completedAt = Instant.now();
        orderToUpdate.setCost(request.cost());
        orderToUpdate.setWorkshop(request.workshop());
        orderToUpdate.setNotes(request.notes());
        orderToUpdate.setOdometerKm(finalOdometer);
        orderToUpdate.setStatus(MaintenanceOrderStatus.COMPLETED);
        orderToUpdate.setCompletedAt(completedAt);
        maintenanceOrderRepository.save(orderToUpdate);

        MaintenancePlan updatedPlan = recalculatePlan(orderToUpdate.getPlanId(), finalOdometer);

        es.bytescolab.msmaintenance.dto.response.RecalculatedDueDatesResponse planSummary = updatedPlan != null
                ? new es.bytescolab.msmaintenance.dto.response.RecalculatedDueDatesResponse(updatedPlan.getId(), updatedPlan.getNextDueAt(), updatedPlan.getNextDueKm())
                : null;

        return new CompletedOrderResponse(
                orderToUpdate.getId(),
                orderToUpdate.getVehicleId(),
                orderToUpdate.getType(),
                orderToUpdate.getStatus(),
                completedAt,
                orderToUpdate.getCost(),
                orderToUpdate.getOdometerKm(),
                orderToUpdate.getWorkshop(),
                planSummary);
    }

    @Override
    @Transactional
    public StartedOrderResponse startOrder(UUID orderId) {
        MaintenanceOrder orderToUpdate = maintenanceOrderRepository
                .findById(orderId).orElseThrow(() -> new OrderNotFound("No existe una orden con el ID proporcionado"));

        if (orderToUpdate.getStatus() != MaintenanceOrderStatus.PENDING) {
            throw new InvalidOrderState("Solo se puede iniciar una orden pendiente");
        }

        final StatusSnapshot vehicle;
        try {
            vehicle = vehicleClient.getVehicle(orderToUpdate.getVehicleId());
        } catch (FeignException.NotFound e) {
            throw new VehicleNotFound("No existe un vehículo con el ID proporcionado");
        } catch (FeignException.ServiceUnavailable | RetryableException e) {
            log.warn("Error al consultar", e.getMessage());
            throw new ServiceUnavailableException("ms-vehicles no responde", e);
        }
        if (vehicle.status() != VehicleStatus.AVAILABLE) {
            throw new VehicleNotAvailable("El vehículo no está disponible");
        }
        try {
            vehicleClient.updateVehicle(
                    orderToUpdate.getVehicleId(),
                    new StatusChangeRequest(VehicleStatus.IN_MAINTENANCE, null));
        } catch (RetryableException e) {
            log.warn("No se pudo conectar con ms-vehicles: {}", e.getMessage());
            throw new ServiceUnavailableException("ms-vehicles no responde", e);
        } catch (FeignException.ServiceUnavailable e) {
            log.warn("ms-vehicles respondió con HTTP 503: {}", e.getMessage());
            throw new ServiceUnavailableException("ms-vehicles no está disponible", e);
        }

        orderToUpdate.setStatus(MaintenanceOrderStatus.IN_PROGRESS);
        orderToUpdate.setStartedAt(Instant.now());
        MaintenanceOrder orderUpdated = maintenanceOrderRepository.save(orderToUpdate);
        return new StartedOrderResponse(
                orderUpdated.getId(),
                orderUpdated.getPlanId(),
                orderUpdated.getVehicleId(),
                orderUpdated.getType(),
                orderUpdated.getStatus(),
                orderUpdated.getScheduledFor(),
                orderUpdated.getStartedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceStatsResponse getStats(MaintenanceStatsRequest request) {
        String granularity = request.granularity().toUpperCase();

        Instant fromInstant = request.from().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = request.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<MaintenanceOrder> completadas = maintenanceOrderRepository.findCompletedInRange(
                request.vehicle(), fromInstant, toInstant);

        long totalOrdenes = completadas.size();
        BigDecimal totalCoste = completadas.stream()
                .map(MaintenanceOrder::getCost)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<LocalDate, MaintenanceAccumulator> buckets = new HashMap<>();
        EnumMap<MaintenanceType, MaintenanceAccumulator> porTipo = new EnumMap<>(MaintenanceType.class);
        for (MaintenanceOrder orden : completadas) {
            LocalDate completadaEn = LocalDate.ofInstant(orden.getCompletedAt(), ZoneOffset.UTC);
            LocalDate periodo = periodoDe(completadaEn, granularity);
            buckets.computeIfAbsent(periodo, k -> new MaintenanceAccumulator()).add(orden);
            porTipo.computeIfAbsent(orden.getType(), k -> new MaintenanceAccumulator()).add(orden);
        }

        List<LocalDate> huecos = rellenarPeriodos(request.from(), request.to(), granularity);
        List<MaintenanceStatsPoint> serie = new ArrayList<>(huecos.size());
        for (LocalDate periodo : huecos) {
            MaintenanceAccumulator acc = buckets.getOrDefault(periodo, new MaintenanceAccumulator());
            serie.add(new MaintenanceStatsPoint(
                    periodo,
                    acc.count,
                    acc.coste.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        List<MaintenanceStatsByType> desglose = new ArrayList<>(porTipo.size());
        for (Map.Entry<MaintenanceType, MaintenanceAccumulator> entry : porTipo.entrySet()) {
            MaintenanceAccumulator acc = entry.getValue();
            desglose.add(new MaintenanceStatsByType(
                    entry.getKey(),
                    acc.count,
                    acc.coste.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long overdue = maintenanceOrderRepository.countOverduePending(request.vehicle(), today);
        long dueIn7Days = maintenanceOrderRepository.countDueWithinSevenDays(
                request.vehicle(), today, today.plusDays(7));

        MaintenanceStatsTotals totales = new MaintenanceStatsTotals(
                totalOrdenes,
                totalCoste.setScale(2, RoundingMode.HALF_UP)
        );
        MaintenanceStatsUpcoming snapshot = new MaintenanceStatsUpcoming(overdue, dueIn7Days);

        return new MaintenanceStatsResponse(
                request.from(),
                request.to(),
                granularity,
                totales,
                serie,
                desglose,
                snapshot
        );
    }

    private LocalDate periodoDe(LocalDate fecha, String granularidad) {
        return switch (granularidad) {
            case "DAY" -> fecha;
            case "WEEK" -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "MONTH" -> fecha.withDayOfMonth(1);
            default -> fecha;
        };
    }

    private List<LocalDate> rellenarPeriodos(LocalDate desde, LocalDate hasta, String granularidad) {
        List<LocalDate> periodos = new ArrayList<>();
        LocalDate cursor = periodoDe(desde, granularidad);
        LocalDate fin = periodoDe(hasta, granularidad);
        while (!cursor.isAfter(fin)) {
            periodos.add(cursor);
            cursor = switch (granularidad) {
                case "DAY" -> cursor.plusDays(1);
                case "WEEK" -> cursor.plusWeeks(1);
                case "MONTH" -> cursor.plusMonths(1);
                default -> cursor.plusDays(1);
            };
        }
        return periodos;
    }

    private static final class MaintenanceAccumulator {
        private long count;
        private BigDecimal coste = BigDecimal.ZERO;

        void add(MaintenanceOrder orden) {
            count++;
            if (orden.getCost() != null) {
                coste = coste.add(orden.getCost());
            }
        }
    }

    private MaintenancePlan recalculatePlan(UUID planId, int finalOdometer) {
        if (planId == null) {
            return null;
        }
        return maintenancePlanRespository.findById(planId).map(plan -> {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            plan.setLastDoneAt(today);
            plan.setLastDoneKm(finalOdometer);
            plan.setNextDueAt(plan.getIntervalDays() != null
                    ? today.plusDays(plan.getIntervalDays())
                    : null);
            plan.setNextDueKm(plan.getIntervalKm() != null
                    ? finalOdometer + plan.getIntervalKm()
                    : null);
            return maintenancePlanRespository.save(plan);
        }).orElse(null);
    }
}
