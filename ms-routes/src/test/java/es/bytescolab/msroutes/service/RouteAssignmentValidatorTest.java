package es.bytescolab.msroutes.service;

import es.bytescolab.msroutes.client.dto.DriverFeignResponse;
import es.bytescolab.msroutes.client.dto.VehicleFeignResponse;
import es.bytescolab.msroutes.enums.VehicleType;
import es.bytescolab.msroutes.exception.DriverNotEligibleException;
import es.bytescolab.msroutes.exception.VehicleNotAvailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteAssignmentValidatorTest {

    private final RouteAssignmentValidator validator = new RouteAssignmentValidator();
    private final UUID driverId = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01");
    private final Instant plannedStart = Instant.parse("2026-10-15T07:00:00Z");

    @Test
    @DisplayName("validateVehicle acepta un vehiculo AVAILABLE")
    void validateVehicleAcceptsAvailable() {
        VehicleFeignResponse vehicle = new VehicleFeignResponse(
                UUID.randomUUID(), "AVAILABLE", "CAR");
        assertDoesNotThrow(() -> validator.validateVehicle(vehicle));
    }

    @Test
    @DisplayName("validateVehicle rechaza un vehiculo OUT_OF_SERVICE")
    void validateVehicleRejectsOutOfService() {
        VehicleFeignResponse vehicle = new VehicleFeignResponse(
                UUID.randomUUID(), "OUT_OF_SERVICE", "CAR");
        assertThrows(VehicleNotAvailableException.class,
                () -> validator.validateVehicle(vehicle));
    }

    @Test
    @DisplayName("validateVehicle rechaza vehiculo nulo como no disponible")
    void validateVehicleRejectsNull() {
        assertThrows(VehicleNotAvailableException.class,
                () -> validator.validateVehicle(null));
    }

    @Test
    @DisplayName("validateDriver acepta conductor ACTIVE con categoria B y licencia vigente")
    void validateDriverAcceptsActiveCategoryB() {
        DriverFeignResponse driver = new DriverFeignResponse(
                driverId, "ACTIVE", "B", LocalDate.now().plusDays(120));
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.CAR, plannedStart));
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.VAN, plannedStart));
    }

    @Test
    @DisplayName("validateDriver rechaza conductor inactivo")
    void validateDriverRejectsInactive() {
        DriverFeignResponse driver = new DriverFeignResponse(
                driverId, "ON_LEAVE", "B", LocalDate.now().plusDays(120));
        assertThrows(DriverNotEligibleException.class,
                () -> validator.validateDriver(driver, VehicleType.CAR, plannedStart));
    }

    @Test
    @DisplayName("validateDriver rechaza conductor con licencia caducada en la fecha de inicio")
    void validateDriverRejectsExpiredLicense() {
        LocalDate yesterday = plannedStart.atZone(java.time.ZoneOffset.UTC).toLocalDate().minusDays(1);
        DriverFeignResponse driver = new DriverFeignResponse(
                driverId, "ACTIVE", "B", yesterday);
        assertThrows(DriverNotEligibleException.class,
                () -> validator.validateDriver(driver, VehicleType.CAR, plannedStart));
    }

    @Test
    @DisplayName("validateDriver acepta licencia que caduca el mismo dia del inicio")
    void validateDriverAcceptsLicenseExpiringToday() {
        LocalDate today = plannedStart.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        DriverFeignResponse driver = new DriverFeignResponse(
                driverId, "ACTIVE", "B", today);
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.CAR, plannedStart));
    }

    @Test
    @DisplayName("Categoria A solo habilita MOTORCYCLE")
    void categoryAOnlyMotorcycle() {
        DriverFeignResponse driver = new DriverFeignResponse(
                driverId, "ACTIVE", "A", LocalDate.now().plusDays(120));
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.MOTORCYCLE, plannedStart));
        assertThrows(DriverNotEligibleException.class,
                () -> validator.validateDriver(driver, VehicleType.CAR, plannedStart));
        assertThrows(DriverNotEligibleException.class,
                () -> validator.validateDriver(driver, VehicleType.VAN, plannedStart));
        assertThrows(DriverNotEligibleException.class,
                () -> validator.validateDriver(driver, VehicleType.TRUCK, plannedStart));
    }

    @Test
    @DisplayName("Categoria B habilita CAR y VAN pero no TRUCK ni MOTORCYCLE")
    void categoryBForCarAndVan() {
        DriverFeignResponse driver = new DriverFeignResponse(
                driverId, "ACTIVE", "B", LocalDate.now().plusDays(120));
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.CAR, plannedStart));
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.VAN, plannedStart));
        assertThrows(DriverNotEligibleException.class,
                () -> validator.validateDriver(driver, VehicleType.TRUCK, plannedStart));
        assertThrows(DriverNotEligibleException.class,
                () -> validator.validateDriver(driver, VehicleType.MOTORCYCLE, plannedStart));
    }

    @Test
    @DisplayName("Categoria C habilita CAR, VAN y TRUCK pero no MOTORCYCLE")
    void categoryCForCarVanTruck() {
        DriverFeignResponse driver = new DriverFeignResponse(
                driverId, "ACTIVE", "C", LocalDate.now().plusDays(120));
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.CAR, plannedStart));
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.VAN, plannedStart));
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.TRUCK, plannedStart));
        assertThrows(DriverNotEligibleException.class,
                () -> validator.validateDriver(driver, VehicleType.MOTORCYCLE, plannedStart));
    }

    @Test
    @DisplayName("validateDriver rechaza driver nulo")
    void validateDriverRejectsNull() {
        assertThrows(DriverNotEligibleException.class,
                () -> validator.validateDriver(null, VehicleType.CAR, plannedStart));
    }

    @Test
    @DisplayName("License exactly on plannedStart equals claim")
    void licenseEqualsPlannedStartPasses() {
        Instant planned = Instant.now().plus(2, ChronoUnit.DAYS);
        LocalDate plannedDate = planned.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        DriverFeignResponse driver = new DriverFeignResponse(
                driverId, "ACTIVE", "B", plannedDate);
        assertDoesNotThrow(() -> validator.validateDriver(driver, VehicleType.CAR, planned));
    }
}