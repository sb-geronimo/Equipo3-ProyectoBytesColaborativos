package es.bytescolab.msroutes.repository;

import es.bytescolab.msroutes.entity.RouteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<RouteEntity, UUID>, JpaSpecificationExecutor<RouteEntity> {

    default Page<RouteEntity> findByFilters(
            UUID vehicle,
            UUID driver,
            es.bytescolab.msroutes.enums.RouteStatus status,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        return this.findAll(RouteSpecifications.withFilters(vehicle, driver, status, from, to), pageable);
    }

    default List<RouteEntity> findActiveOverlappingCandidates(
            UUID vehicleId,
            UUID driverId,
            Instant newEnd
    ) {
        return this.findAll(RouteSpecifications.activeOverlapping(vehicleId, driverId, newEnd));
    }

    default List<RouteEntity> findCompletedInRange(
            UUID vehicle,
            Instant from,
            Instant to
    ) {
        return this.findAll(RouteSpecifications.completedInRange(vehicle, from, to));
    }
}
