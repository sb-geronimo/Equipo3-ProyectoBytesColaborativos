package es.bytescolab.msroutes.service.impl;

import es.bytescolab.msroutes.client.DriverClient;
import es.bytescolab.msroutes.client.VehicleClient;
import es.bytescolab.msroutes.dto.request.RouteCompleteRequest;
import es.bytescolab.msroutes.dto.request.RouteCreateRequest;
import es.bytescolab.msroutes.dto.request.RouteStatsRequest;
import es.bytescolab.msroutes.dto.request.VehicleStatusUpdateRequest;
import es.bytescolab.msroutes.dto.response.DriverDetailResponse;
import es.bytescolab.msroutes.dto.response.RouteResponse;
import es.bytescolab.msroutes.dto.response.RouteStatsPoint;
import es.bytescolab.msroutes.dto.response.RouteStatsResponse;
import es.bytescolab.msroutes.dto.response.RouteStatsTotals;
import es.bytescolab.msroutes.dto.response.VehicleDetailResponse;
import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.DriverStatus;
import es.bytescolab.msroutes.enums.LicenseCategory;
import es.bytescolab.msroutes.enums.RouteStatus;
import es.bytescolab.msroutes.enums.VehicleStatus;
import es.bytescolab.msroutes.enums.VehicleType;
import es.bytescolab.msroutes.exception.DriverNotEligibleException;
import es.bytescolab.msroutes.exception.DriverNotFoundException;
import es.bytescolab.msroutes.exception.InvalidRouteStateException;
import es.bytescolab.msroutes.exception.RouteNotFoundException;
import es.bytescolab.msroutes.exception.RouteOverlapException;
import es.bytescolab.msroutes.exception.RouteServiceUnavailableException;
import es.bytescolab.msroutes.exception.VehicleNotAvailableException;
import es.bytescolab.msroutes.exception.VehicleNotFoundException;
import es.bytescolab.msroutes.mapper.RouteMapper;
import es.bytescolab.msroutes.repository.RouteRepository;
import es.bytescolab.msroutes.service.RouteService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private static final String MS_VEHICLES = "ms-vehicles";
    private static final String MS_DRIVERS = "ms-drivers";

    private final RouteMapper mapper;
    private final DriverClient driverClient;
    private final VehicleClient vehicleClient;
    private final RouteRepository repository;

    @Override
    @Transactional
    public RouteResponse create(RouteCreateRequest request, UUID userId) {
        VehicleDetailResponse vehicle = this.fetchVehicle(request.vehicleId());
        this.validateVehicleAvailable(vehicle);

        DriverDetailResponse driver = this.fetchDriver(request.driverId());
        this.validateDriverEligibility(driver, request, vehicle.type());

        Instant newEnd = request.plannedStart()
                .plus((long) request.estimatedDurationMin(), ChronoUnit.MINUTES);
        ensureNoOverlap(request.vehicleId(), request.driverId(),
                request.plannedStart(), newEnd);

        RouteEntity entity = mapper.toEntity(request);
        entity.setCreatedBy(userId);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RouteResponse> findAll(UUID vehicle, UUID driver, RouteStatus status,
                                       LocalDate from, LocalDate to, int page, int size) {
        Instant fromInstant = from == null ? null
                : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to == null ? null
                : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "plannedStart"));
        return repository.findByFilters(vehicle, driver, status, fromInstant, toInstant, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse findById(UUID routeId) {
        return mapper.toResponse(loadRoute(routeId));
    }

    @Override
    @Transactional
    public RouteResponse start(UUID routeId) {
        RouteEntity route = this.loadRoute(routeId);
        if (route.getStatus() != RouteStatus.PLANNED) {
            throw new InvalidRouteStateException(
                    "Solo se pueden iniciar rutas en estado PLANNED");
        }

        VehicleDetailResponse vehicle = this.fetchVehicle(route.getVehicleId());
        if (vehicle.status() != VehicleStatus.AVAILABLE) {
            throw new VehicleNotAvailableException(
                    "El vehículo no está disponible (estado actual: " + vehicle.status() + ")");
        }

        try {
            log.info("Actualizando estado del vehículo {} a IN_USE", vehicle.id());
            vehicleClient.updateStatus(vehicle.id(),
                    new VehicleStatusUpdateRequest(VehicleStatus.IN_USE, null));
        } catch (FeignException ex) {
            log.info("Fallo al cambiar el estado del vehículo a IN_USE: {}", ex.getMessage());
            throw new RouteServiceUnavailableException(MS_VEHICLES,
                    "ms-vehicles no pudo cambiar el estado del vehículo a IN_USE");
        }

        try {
            route.setStatus(RouteStatus.IN_PROGRESS);
            route.setStartedAt(Instant.now());
            route.setStartOdometerKm(vehicle.odometerKm());
            route.setNew(false);
            return mapper.toResponse(repository.save(route));
        } catch (RuntimeException ex) {
            log.error("Fallo al guardar la ruta como IN_PROGRESS. Compensando vehículo.", ex);
            try {
                vehicleClient.updateStatus(vehicle.id(),
                        new VehicleStatusUpdateRequest(
                                VehicleStatus.AVAILABLE, vehicle.odometerKm()));
            } catch (FeignException compensateEx) {
                log.error("Compensación fallida al devolver el vehículo a AVAILABLE", compensateEx);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public RouteResponse complete(UUID routeId, RouteCompleteRequest request) {
        RouteEntity route = loadRoute(routeId);
        if (route.getStatus() != RouteStatus.IN_PROGRESS) {
            throw new InvalidRouteStateException(
                    "Solo se pueden completar rutas en estado IN_PROGRESS");
        }

        BigDecimal actualDistance = request.actualDistanceKm()
                .setScale(2, RoundingMode.HALF_UP);
        int additionalKm = (int) Math.round(actualDistance.doubleValue());
        int endOdometer = route.getStartOdometerKm() + additionalKm;

        try {
            vehicleClient.updateStatus(route.getVehicleId(),
                    new VehicleStatusUpdateRequest(VehicleStatus.AVAILABLE, endOdometer));
        } catch (FeignException ex) {
            throw new RouteServiceUnavailableException(MS_VEHICLES,
                    "ms-vehicles no pudo devolver el vehículo a AVAILABLE");
        }

        route.setStatus(RouteStatus.COMPLETED);
        route.setEndedAt(Instant.now());
        route.setEndOdometerKm(endOdometer);
        route.setActualDistanceKm(actualDistance);
        route.setNotes(request.notes());
        route.setNew(false);
        return mapper.toResponse(repository.save(route));
    }

    @Override
    @Transactional(readOnly = true)
    public RouteStatsResponse getStats(RouteStatsRequest request) {
        String granularity = (request.granularity() == null
                || request.granularity().isBlank())
                ? "DAY" : request.granularity().toUpperCase();

        Instant fromInstant = request.from().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = request.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<RouteEntity> routes = repository.findCompletedInRange(
                request.vehicle(), fromInstant, toInstant);

        long totalRoutes = routes.size();
        BigDecimal totalDistance = routes.stream()
                .map(RouteEntity::getActualDistanceKm)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<LocalDate, RouteAccumulator> buckets = new HashMap<>();
        for (RouteEntity route : routes) {
            LocalDate endedOn = LocalDate.ofInstant(route.getEndedAt(), ZoneOffset.UTC);
            LocalDate period = periodOf(endedOn, granularity);
            buckets.computeIfAbsent(period, k -> new RouteAccumulator()).add(route);
        }

        List<LocalDate> filled = fillPeriods(request.from(), request.to(), granularity);
        List<RouteStatsPoint> series = new ArrayList<>(filled.size());
        for (LocalDate period : filled) {
            RouteAccumulator acc = buckets.getOrDefault(period, new RouteAccumulator());
            series.add(new RouteStatsPoint(
                    period,
                    acc.count,
                    acc.distance.setScale(2, RoundingMode.HALF_UP),
                    acc.count == 0 ? 0
                            : (int) Math.round(acc.totalDuration / (double) acc.count)
            ));
        }

        RouteStatsTotals totals = new RouteStatsTotals(
                totalRoutes,
                totalDistance.setScale(2, RoundingMode.HALF_UP)
        );

        return new RouteStatsResponse(
                request.from(),
                request.to(),
                granularity,
                totals,
                series
        );
    }

    private RouteEntity loadRoute(UUID routeId) {
        return repository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(routeId));
    }

    private VehicleDetailResponse fetchVehicle(UUID vehicleId) {
        try {
            return vehicleClient.getVehicle(vehicleId);
        } catch (FeignException.NotFound ex) {
            throw new VehicleNotFoundException(vehicleId);
        } catch (FeignException ex) {
            throw new RouteServiceUnavailableException(MS_VEHICLES,
                    "ms-vehicles no está disponible");
        }
    }

    private DriverDetailResponse fetchDriver(UUID driverId) {
        try {
            return driverClient.getDriver(driverId);
        } catch (FeignException.NotFound ex) {
            throw new DriverNotFoundException(driverId);
        } catch (FeignException ex) {
            throw new RouteServiceUnavailableException(MS_DRIVERS,
                    "ms-drivers no está disponible");
        }
    }

    private void validateVehicleAvailable(VehicleDetailResponse vehicle) {
        if (vehicle.status() == VehicleStatus.OUT_OF_SERVICE) {
            throw new VehicleNotAvailableException("El vehículo está fuera de servicio");
        }
    }

    private void validateDriverEligibility(DriverDetailResponse driver,
                                           RouteCreateRequest request,
                                           VehicleType vehicleType) {
        if (driver.status() != DriverStatus.ACTIVE) {
            throw new DriverNotEligibleException(
                    "El conductor no está activo (estado actual: " + driver.status() + ")");
        }
        LocalDate plannedDate = request.plannedStart().atZone(ZoneOffset.UTC).toLocalDate();
        if (driver.licenseExpiresAt() == null
                || !driver.licenseExpiresAt().isAfter(plannedDate)) {
            throw new DriverNotEligibleException(
                    "La licencia del conductor no está vigente en la fecha planificada de inicio");
        }
        if (!isCompatible(driver.licenseCategory(), vehicleType)) {
            throw new DriverNotEligibleException(
                    "La categoría de licencia " + driver.licenseCategory()
                            + " no es compatible con el tipo de vehículo " + vehicleType);
        }
    }

    private boolean isCompatible(LicenseCategory category, VehicleType type) {
        if (category == null || type == null) {
            return false;
        }
        return switch (category) {
            case A -> type == VehicleType.MOTORCYCLE;
            case B -> type == VehicleType.CAR || type == VehicleType.VAN;
            case C -> type == VehicleType.CAR || type == VehicleType.VAN || type == VehicleType.TRUCK;
        };
    }

    private void ensureNoOverlap(UUID vehicleId, UUID driverId, Instant newStart, Instant newEnd) {
        List<RouteEntity> candidates = repository.findActiveOverlappingCandidates(
                vehicleId, driverId, newEnd);
        for (RouteEntity existing : candidates) {
            Instant existingEnd = existing.getPlannedStart()
                    .plus((long) existing.getEstimatedDurationMin(), ChronoUnit.MINUTES);
            if (existingEnd.isAfter(newStart)) {
                throw new RouteOverlapException(
                        "La ruta se solapa con otra ruta activa del mismo vehículo o conductor");
            }
        }
    }

    private LocalDate periodOf(LocalDate date, String granularity) {
        return switch (granularity) {
            case "DAY" -> date;
            case "WEEK" -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "MONTH" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    private List<LocalDate> fillPeriods(LocalDate from, LocalDate to, String granularity) {
        List<LocalDate> periods = new ArrayList<>();
        LocalDate cursor = periodOf(from, granularity);
        LocalDate end = periodOf(to, granularity);
        while (!cursor.isAfter(end)) {
            periods.add(cursor);
            cursor = switch (granularity) {
                case "DAY" -> cursor.plusDays(1);
                case "WEEK" -> cursor.plusWeeks(1);
                case "MONTH" -> cursor.plusMonths(1);
                default -> cursor.plusDays(1);
            };
        }
        return periods;
    }

    private static final class RouteAccumulator {
        private long count;
        private long totalDuration;
        private BigDecimal distance = BigDecimal.ZERO;

        void add(RouteEntity route) {
            count++;
            totalDuration += route.getEstimatedDurationMin() == null
                    ? 0 : route.getEstimatedDurationMin();
            if (route.getActualDistanceKm() != null) {
                distance = distance.add(route.getActualDistanceKm());
            }
        }
    }
}
