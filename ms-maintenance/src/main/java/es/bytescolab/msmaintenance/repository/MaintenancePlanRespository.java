package es.bytescolab.msmaintenance.repository;

import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenancePlanRespository extends JpaRepository<MaintenancePlan, UUID> {

    boolean existsByVehicleIdAndTypeAndActiveTrue(UUID vehicleId, MaintenanceType type);

    @Query("""
            SELECT p FROM MaintenancePlan p
            WHERE (:vehicleId IS NULL OR p.vehicleId = :vehicleId)
              AND (:type IS NULL OR p.type = :type)
              AND (:active IS NULL OR p.active = :active)
            """)
    List<MaintenancePlan> search(@Param("vehicleId") UUID vehicleId,
                                 @Param("type") MaintenanceType type,
                                 @Param("active") Boolean active);
}
