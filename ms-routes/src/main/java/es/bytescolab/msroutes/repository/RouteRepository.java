package es.bytescolab.msroutes.repository;

import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RouteRepository
        extends JpaRepository<RouteEntity, UUID>, JpaSpecificationExecutor<RouteEntity> {

    @Query("SELECT r FROM RouteEntity r "
            + "WHERE (r.vehicleId = :vehicleId OR r.driverId = :driverId) "
            + "AND r.status IN :statuses "
            + "AND r.plannedStart < :plannedStartBefore")
    List<RouteEntity> findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
            @Param("vehicleId") UUID vehicleId,
            @Param("driverId") UUID driverId,
            @Param("statuses") Collection<RouteStatus> statuses,
            @Param("plannedStartBefore") Instant plannedStartBefore
    );
}