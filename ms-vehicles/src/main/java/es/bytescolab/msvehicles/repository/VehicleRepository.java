package es.bytescolab.msvehicles.repository;

import es.bytescolab.msvehicles.entity.Vehicle;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID>,
        JpaSpecificationExecutor<Vehicle> {

    long countByStatus(VehicleStatus status);

    long countByType(VehicleType type);
}
