package es.bytescolab.msroutes.service.impl;

import es.bytescolab.msroutes.client.DriverFeignClient;
import es.bytescolab.msroutes.client.VehicleFeignClient;
import es.bytescolab.msroutes.client.dto.DriverFeignResponse;
import es.bytescolab.msroutes.client.dto.VehicleFeignResponse;
import es.bytescolab.msroutes.dto.request.CreateRouteRequest;
import es.bytescolab.msroutes.dto.response.PageResponse;
import es.bytescolab.msroutes.dto.response.RouteDetailResponse;
import es.bytescolab.msroutes.dto.response.RouteSummaryResponse;
import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.RouteStatus;
import es.bytescolab.msroutes.enums.VehicleType;
import es.bytescolab.msroutes.exception.RouteNotFoundException;
import es.bytescolab.msroutes.exception.RouteOverlapException;
import es.bytescolab.msroutes.exception.ServiceUnavailableException;
import es.bytescolab.msroutes.mapper.RouteMapper;
import es.bytescolab.msroutes.repository.RouteRepository;
import es.bytescolab.msroutes.service.RouteAssignmentValidator;
import es.bytescolab.msroutes.service.RouteService;
import es.bytescolab.msroutes.specification.RouteSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private static final String INTERNAL_PRINCIPAL = "internal";

    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;
    private final VehicleFeignClient vehicleFeignClient;
    private final DriverFeignClient driverFeignClient;
    private final RouteAssignmentValidator routeAssignmentValidator;

    @Override
    public RouteDetailResponse create(CreateRouteRequest request) {
        Objects.requireNonNull(request, "CreateRouteRequest no puede ser nulo");

        log.info("Planning new route — vehicleId={}, driverId={}", request.vehicleId(), request.driverId());

        VehicleFeignResponse vehicle = vehicleFeignClient.findById(request.vehicleId());
        routeAssignmentValidator.validateVehicle(vehicle);

        VehicleType vehicleType = parseVehicleType(vehicle.type());

        DriverFeignResponse driver = driverFeignClient.findById(request.driverId());
        routeAssignmentValidator.validateDriver(driver, vehicleType, request.plannedStart());

        ensureNoOverlap(request.vehicleId(), request.driverId(), request.plannedStart(), request.estimatedDurationMin());

        RouteEntity entity = routeMapper.toEntity(request);
        entity.setCreatedBy(resolveCreatedBy());

        RouteEntity saved = routeRepository.save(entity);
        log.debug("Route saved — id={}, status={}", saved.getId(), saved.getStatus());

        return routeMapper.toDetailResponse(saved);
    }

    @Override
    public RouteDetailResponse findById(UUID routeId) {
        log.debug("Find route — id={}", routeId);
        RouteEntity entity = routeRepository.findById(routeId)
                .orElseThrow(RouteNotFoundException::new);
        return routeMapper.toDetailResponse(entity);
    }

    @Override
    public PageResponse<RouteSummaryResponse> findAll(UUID vehicleId,
                                                     UUID driverId,
                                                     RouteStatus status,
                                                     LocalDate from,
                                                     LocalDate to,
                                                     Pageable pageable) {
        Specification<RouteEntity> specification = Specification
                .where(RouteSpecifications.hasVehicle(vehicleId))
                .and(RouteSpecifications.hasDriver(driverId))
                .and(RouteSpecifications.hasStatus(status))
                .and(RouteSpecifications.plannedStartBetween(from, to));

        Pageable effectivePageable = pageable.getSort().isUnsorted()
                ? org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "plannedStart"))
                : pageable;

        Page<RouteEntity> routes = routeRepository.findAll(specification, effectivePageable);
        return PageResponse.from(routes, routeMapper::toSummaryResponse);
    }

    private void ensureNoOverlap(UUID vehicleId, UUID driverId, Instant plannedStart, Integer estimatedDurationMin) {
        Instant newEnd = plannedStart.plus(Duration.ofMinutes(estimatedDurationMin));
        List<RouteEntity> candidates = routeRepository.findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
                vehicleId,
                driverId,
                EnumSet.of(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS),
                newEnd
        );

        boolean overlap = candidates.stream().anyMatch(candidate -> {
            Instant candidateStart = candidate.getPlannedStart();
            Instant candidateEnd = candidateStart.plus(Duration.ofMinutes(candidate.getEstimatedDurationMin()));
            return candidateStart.isBefore(newEnd) && candidateEnd.isAfter(plannedStart);
        });

        if (overlap) {
            throw new RouteOverlapException();
        }
    }

    private VehicleType parseVehicleType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ServiceUnavailableException("ms-vehicles", "tipo de vehiculo ausente en la respuesta");
        }
        try {
            return VehicleType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceUnavailableException("ms-vehicles",
                    "tipo de vehiculo no reconocido: " + raw);
        }
    }

    private UUID resolveCreatedBy() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        String name = authentication.getName();
        if (INTERNAL_PRINCIPAL.equals(name)) {
            return null;
        }
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}