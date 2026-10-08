package es.bytescolab.msroutes.service;

import es.bytescolab.msroutes.client.dto.DriverFeignResponse;
import es.bytescolab.msroutes.client.dto.VehicleFeignResponse;
import es.bytescolab.msroutes.enums.LicenseCategory;
import es.bytescolab.msroutes.enums.VehicleType;
import es.bytescolab.msroutes.exception.DriverNotEligibleException;
import es.bytescolab.msroutes.exception.VehicleNotAvailableException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;

@Component
public class RouteAssignmentValidator {

    private static final String VEHICLE_OUT_OF_SERVICE = "OUT_OF_SERVICE";
    private static final String DRIVER_ACTIVE = "ACTIVE";
    private static final String INCOMPATIBLE_LICENSE_CATEGORY =
            "La categoria de la licencia no es compatible con el tipo de vehiculo";
    private static final String INACTIVE_DRIVER =
            "El conductor no esta activo y no puede asignarse a una ruta";
    private static final String EXPIRED_LICENSE =
            "La licencia del conductor esta caducada en la fecha de inicio de la ruta";

    private static final Set<VehicleType> A_ALLOWED = EnumSet.of(VehicleType.MOTORCYCLE);
    private static final Set<VehicleType> B_ALLOWED = EnumSet.of(VehicleType.CAR, VehicleType.VAN);
    private static final Set<VehicleType> C_ALLOWED = EnumSet.of(VehicleType.CAR, VehicleType.VAN, VehicleType.TRUCK);

    public void validateVehicle(VehicleFeignResponse vehicle) {
        if (vehicle == null) {
            throw new VehicleNotAvailableException();
        }
        if (VEHICLE_OUT_OF_SERVICE.equals(vehicle.status())) {
            throw new VehicleNotAvailableException();
        }
    }

    public void validateDriver(DriverFeignResponse driver, VehicleType vehicleType, Instant plannedStart) {
        if (driver == null) {
            throw new DriverNotEligibleException(null);
        }
        if (!DRIVER_ACTIVE.equals(driver.status())) {
            throw new DriverNotEligibleException(INACTIVE_DRIVER);
        }

        LicenseCategory category = parseLicenseCategory(driver.licenseCategory());
        if (!isCompatible(category, vehicleType)) {
            throw new DriverNotEligibleException(INCOMPATIBLE_LICENSE_CATEGORY);
        }

        if (driver.licenseExpiresAt() == null
                || driver.licenseExpiresAt().isBefore(plannedStart.atZone(ZoneOffset.UTC).toLocalDate())) {
            throw new DriverNotEligibleException(EXPIRED_LICENSE);
        }
    }

    private boolean isCompatible(LicenseCategory category, VehicleType vehicleType) {
        if (category == null || vehicleType == null) {
            return false;
        }
        return switch (category) {
            case A -> A_ALLOWED.contains(vehicleType);
            case B -> B_ALLOWED.contains(vehicleType);
            case C -> C_ALLOWED.contains(vehicleType);
        };
    }

    private LicenseCategory parseLicenseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LicenseCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}