package es.bytescolab.msdrivers.repository;

import es.bytescolab.msdrivers.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<DriverEntity, UUID> {

    Optional<DriverEntity> findByEmail(String email);

    Optional<DriverEntity> findByLicenseNumber(String licenseNumber);

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);
}
