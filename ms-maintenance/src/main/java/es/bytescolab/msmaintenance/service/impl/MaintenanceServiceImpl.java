package es.bytescolab.msmaintenance.service.impl;

import es.bytescolab.msmaintenance.client.VehicleClient;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusChangeRequest;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusSnapshot;
import es.bytescolab.msmaintenance.client.vehicle.enums.VehicleStatus;
import es.bytescolab.msmaintenance.dto.request.CompleteOrderRequest;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
