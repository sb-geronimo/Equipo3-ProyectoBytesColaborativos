package es.bytescolab.msroutes.repository;

import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RouteRepository
        extends JpaRepository<RouteEntity, UUID>, JpaSpecificationExecutor<RouteEntity> {

    List<RouteEntity> findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
            UUID vehicleId,
            UUID driverId,
            Collection<RouteStatus> statuses,
            Instant plannedStartBefore
    );
}