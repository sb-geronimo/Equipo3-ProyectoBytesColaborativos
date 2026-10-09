package es.bytescolab.msmaintenance.repository;

import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaintenancePlanRespository extends JpaRepository<MaintenancePlan, UUID>,
        JpaSpecificationExecutor<MaintenancePlan> {

    boolean existsByVehicleIdAndTypeAndActiveTrue(UUID vehicleId, MaintenanceType type);
}
