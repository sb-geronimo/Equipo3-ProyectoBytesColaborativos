package es.bytescolab.msroutes.service.impl;

import es.bytescolab.msroutes.client.DriverClient;
import es.bytescolab.msroutes.client.VehicleClient;
import es.bytescolab.msroutes.dto.request.RouteCompleteRequest;
import es.bytescolab.msroutes.dto.request.RouteCreateRequest;
import es.bytescolab.msroutes.dto.response.DriverDetailResponse;
import es.bytescolab.msroutes.dto.response.RouteResponse;
import es.bytescolab.msroutes.dto.response.VehicleDetailResponse;
import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.DriverStatus;
import es.bytescolab.msroutes.enums.LicenseCategory;
import es.bytescolab.msroutes.enums.RouteStatus;
import es.bytescolab.msroutes.enums.VehicleStatus;
import es.bytescolab.msroutes.enums.VehicleType;
import es.bytescolab.msroutes.exception.DriverNotEligibleException;
import es.bytescolab.msroutes.exception.InvalidRouteStateException;
import es.bytescolab.msroutes.exception.RouteOverlapException;
import es.bytescolab.msroutes.exception.VehicleNotAvailableException;
import es.bytescolab.msroutes.mapper.RouteMapper;
import es.bytescolab.msroutes.repository.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private RouteMapper mapper;

    @Mock
    private DriverClient driverClient;

    @Mock
    private VehicleClient vehicleClient;

    @Mock
    private RouteRepository repository;

    @InjectMocks
    private RouteServiceImpl routeService;

    private UUID vehicleId;
    private UUID driverId;
    private UUID routeId;
    private UUID userId;
    private VehicleDetailResponse availableVan;
    private DriverDetailResponse activeDriver;
    private RouteCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61");
        driverId = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01");
        routeId = UUID.fromString("5b9e2c10-7f4a-4d55-b1a0-3c8e6f7a9201");
        userId = UUID.fromString("9a4d2e10-7c4a-4b55-b1a0-3c8e6f7a9210");

        availableVan = VehicleDetailResponse.builder()
                .id(vehicleId)
                .plate("4821-KDF")
                .make("Ford")
                .model("Transit")
                .year(2022)
                .type(VehicleType.VAN)
                .fuelType("DIESEL")
                .tankCapacityL(80)
                .odometerKm(45210)
                .status(VehicleStatus.AVAILABLE)
                .build();

        activeDriver = DriverDetailResponse.builder()
                .id(driverId)
                .fullName("Sara Ibáñez")
                .email("sara.ibanez@fleetcontrol.com")
                .phone("+34 622 456 789")
                .licenseNumber("B-5120377")
                .licenseCategory(LicenseCategory.B)
                .licenseExpiresAt(LocalDate.parse("2027-01-01"))
                .status(DriverStatus.ACTIVE)
                .build();

        createRequest = new RouteCreateRequest(
                vehicleId,
                driverId,
                "Madrid Centro Logístico",
                "Valencia Puerto",
                Instant.parse("2026-10-06T07:00:00Z"),
                240,
                new BigDecimal("355.0")
        );
    }

    @Test
    @DisplayName("start should transition PLANNED to IN_PROGRESS when vehicle is AVAILABLE and persist startOdometer")
    void start_should_transitionToInProgress_when_routePlannedAndVehicleAvailable() {
        RouteEntity existing = buildRouteEntity(RouteStatus.PLANNED);
        when(repository.findById(routeId)).thenReturn(Optional.of(existing));
        when(vehicleClient.getVehicle(vehicleId)).thenReturn(availableVan);
        when(vehicleClient.updateStatus(eq(vehicleId), any())).thenReturn(availableVan);
        when(repository.save(any(RouteEntity.class))).thenReturn(existing);
        when(mapper.toResponse(any(RouteEntity.class))).thenReturn(buildRouteResponse(RouteStatus.IN_PROGRESS));

        RouteResponse response = routeService.start(routeId);

        assertThat(response.status()).isEqualTo(RouteStatus.IN_PROGRESS);
        assertThat(existing.getStatus()).isEqualTo(RouteStatus.IN_PROGRESS);
        assertThat(existing.getStartedAt()).isNotNull();
        assertThat(existing.getStartOdometerKm()).isEqualTo(45210);
    }

    @Test
    @DisplayName("start should throw InvalidRouteStateException when route is already IN_PROGRESS")
    void start_should_throwInvalidRouteState_when_routeAlreadyInProgress() {
        RouteEntity existing = buildRouteEntity(RouteStatus.IN_PROGRESS);
        when(repository.findById(routeId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> routeService.start(routeId))
                .isInstanceOf(InvalidRouteStateException.class)
                .hasMessageContaining("PLANNED");
    }

    @Test
    @DisplayName("start should throw VehicleNotAvailableException when vehicle is IN_USE")
    void start_should_throwVehicleNotAvailable_when_vehicleInUse() {
        RouteEntity existing = buildRouteEntity(RouteStatus.PLANNED);
        VehicleDetailResponse inUseVehicle = VehicleDetailResponse.builder()
                .id(vehicleId)
                .plate("4821-KDF")
                .type(VehicleType.VAN)
                .odometerKm(45210)
                .status(VehicleStatus.IN_USE)
                .build();

        when(repository.findById(routeId)).thenReturn(Optional.of(existing));
        when(vehicleClient.getVehicle(vehicleId)).thenReturn(inUseVehicle);

        assertThatThrownBy(() -> routeService.start(routeId))
                .isInstanceOf(VehicleNotAvailableException.class)
                .hasMessageContaining("IN_USE");
    }

    @Test
    @DisplayName("complete should transition IN_PROGRESS to COMPLETED applying rounded odometer delta")
    void complete_should_transitionToCompleted_when_routeInProgressAndOdometerRounded() {
        RouteEntity existing = buildRouteEntity(RouteStatus.IN_PROGRESS);
        existing.setStartOdometerKm(45210);
        when(repository.findById(routeId)).thenReturn(Optional.of(existing));
        when(vehicleClient.updateStatus(eq(vehicleId), any())).thenReturn(availableVan);
        when(repository.save(any(RouteEntity.class))).thenReturn(existing);
        when(mapper.toResponse(any(RouteEntity.class))).thenReturn(buildRouteResponse(RouteStatus.COMPLETED));

        RouteCompleteRequest completeRequest = new RouteCompleteRequest(
                new BigDecimal("362.40"), "Sin incidencias"
        );

        RouteResponse response = routeService.complete(routeId, completeRequest);

        assertThat(response.status()).isEqualTo(RouteStatus.COMPLETED);
        assertThat(existing.getStatus()).isEqualTo(RouteStatus.COMPLETED);
        assertThat(existing.getEndOdometerKm()).isEqualTo(45210 + 362);
        assertThat(existing.getActualDistanceKm()).isEqualByComparingTo("362.40");
        assertThat(existing.getNotes()).isEqualTo("Sin incidencias");
        assertThat(existing.getEndedAt()).isNotNull();
    }

    @Test
    @DisplayName("complete should throw InvalidRouteStateException when route is PLANNED")
    void complete_should_throwInvalidRouteState_when_routePlanned() {
        RouteEntity existing = buildRouteEntity(RouteStatus.PLANNED);
        when(repository.findById(routeId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> routeService.complete(routeId,
                new RouteCompleteRequest(new BigDecimal("10.00"), null)))
                .isInstanceOf(InvalidRouteStateException.class)
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    @DisplayName("create should throw DriverNotEligibleException when driver status is not ACTIVE")
    void create_should_throwDriverNotEligible_when_driverInactive() {
        DriverDetailResponse suspendedDriver = DriverDetailResponse.builder()
                .id(driverId)
                .fullName(activeDriver.fullName())
                .email(activeDriver.email())
                .phone(activeDriver.phone())
                .licenseNumber(activeDriver.licenseNumber())
                .licenseCategory(LicenseCategory.B)
                .licenseExpiresAt(LocalDate.parse("2027-01-01"))
                .status(DriverStatus.SUSPENDED)
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(availableVan);
        when(driverClient.getDriver(driverId)).thenReturn(suspendedDriver);

        assertThatThrownBy(() -> routeService.create(createRequest, userId))
                .isInstanceOf(DriverNotEligibleException.class)
                .hasMessageContaining("activo");
    }

    @Test
    @DisplayName("create should throw DriverNotEligibleException when license expires on or before planned date")
    void create_should_throwDriverNotEligible_when_licenseExpired() {
        DriverDetailResponse expiredLicense = DriverDetailResponse.builder()
                .id(driverId)
                .fullName(activeDriver.fullName())
                .email(activeDriver.email())
                .phone(activeDriver.phone())
                .licenseNumber(activeDriver.licenseNumber())
                .licenseCategory(LicenseCategory.B)
                .licenseExpiresAt(LocalDate.parse("2026-09-01"))
                .status(DriverStatus.ACTIVE)
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(availableVan);
        when(driverClient.getDriver(driverId)).thenReturn(expiredLicense);

        assertThatThrownBy(() -> routeService.create(createRequest, userId))
                .isInstanceOf(DriverNotEligibleException.class)
                .hasMessageContaining("vigente");
    }

    @Test
    @DisplayName("create should throw DriverNotEligibleException when license category is incompatible with vehicle type")
    void create_should_throwDriverNotEligible_when_categoryIncompatibleWithVehicleType() {
        DriverDetailResponse categoryAMismatch = DriverDetailResponse.builder()
                .id(driverId)
                .fullName(activeDriver.fullName())
                .email(activeDriver.email())
                .phone(activeDriver.phone())
                .licenseNumber(activeDriver.licenseNumber())
                .licenseCategory(LicenseCategory.A)
                .licenseExpiresAt(LocalDate.parse("2027-01-01"))
                .status(DriverStatus.ACTIVE)
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(availableVan);
        when(driverClient.getDriver(driverId)).thenReturn(categoryAMismatch);

        assertThatThrownBy(() -> routeService.create(createRequest, userId))
                .isInstanceOf(DriverNotEligibleException.class)
                .hasMessageContaining("compatible");
    }

    @ParameterizedTest(name = "category {0} with type {1} is compatible: {2}")
    @MethodSource("compatibilityCases")
    @DisplayName("validateDriverEligibility should accept only compatible LicenseCategory × VehicleType pairs")
    void validateDriverEligibility_should_acceptOnlyCompatibleCategoryAndType(
            LicenseCategory category, VehicleType type, boolean expected) {
        DriverDetailResponse categoryDriver = DriverDetailResponse.builder()
                .id(driverId)
                .fullName(activeDriver.fullName())
                .email(activeDriver.email())
                .phone(activeDriver.phone())
                .licenseNumber(activeDriver.licenseNumber())
                .licenseCategory(category)
                .licenseExpiresAt(LocalDate.parse("2027-01-01"))
                .status(DriverStatus.ACTIVE)
                .build();
        VehicleDetailResponse typedVehicle = VehicleDetailResponse.builder()
                .id(vehicleId)
                .plate(availableVan.plate())
                .type(type)
                .fuelType("DIESEL")
                .tankCapacityL(80)
                .odometerKm(45210)
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(typedVehicle);
        when(driverClient.getDriver(driverId)).thenReturn(categoryDriver);

        if (expected) {
            when(repository.findActiveOverlappingCandidates(any(), any(), any())).thenReturn(List.of());
            when(mapper.toEntity(any(RouteCreateRequest.class))).thenReturn(buildRouteEntity(RouteStatus.PLANNED));
            when(repository.save(any(RouteEntity.class))).thenReturn(buildRouteEntity(RouteStatus.PLANNED));
            when(mapper.toResponse(any(RouteEntity.class))).thenReturn(buildRouteResponse(RouteStatus.PLANNED));

            assertThatCode(() -> routeService.create(createRequest, userId))
                    .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> routeService.create(createRequest, userId))
                    .isInstanceOf(DriverNotEligibleException.class)
                    .hasMessageContaining("compatible");
        }
    }

    static Stream<Arguments> compatibilityCases() {
        return Stream.of(
                Arguments.of(LicenseCategory.A, VehicleType.MOTORCYCLE, true),
                Arguments.of(LicenseCategory.A, VehicleType.CAR, false),
                Arguments.of(LicenseCategory.A, VehicleType.VAN, false),
                Arguments.of(LicenseCategory.A, VehicleType.TRUCK, false),
                Arguments.of(LicenseCategory.B, VehicleType.CAR, true),
                Arguments.of(LicenseCategory.B, VehicleType.VAN, true),
                Arguments.of(LicenseCategory.B, VehicleType.TRUCK, false),
                Arguments.of(LicenseCategory.B, VehicleType.MOTORCYCLE, false),
                Arguments.of(LicenseCategory.C, VehicleType.CAR, true),
                Arguments.of(LicenseCategory.C, VehicleType.VAN, true),
                Arguments.of(LicenseCategory.C, VehicleType.TRUCK, true),
                Arguments.of(LicenseCategory.C, VehicleType.MOTORCYCLE, false)
        );
    }

    @Test
    @DisplayName("create should throw RouteOverlapException when an active route for the same driver overlaps the new window")
    void create_should_throwRouteOverlap_when_sameDriverOverlappingWindow() {
        RouteEntity overlappingRoute = RouteEntity.builder()
                .id(UUID.randomUUID())
                .vehicleId(UUID.randomUUID())
                .driverId(driverId)
                .origin("Otro origen")
                .destination("Otro destino")
                .plannedStart(Instant.parse("2026-10-06T08:00:00Z"))
                .estimatedDurationMin(60)
                .plannedDistanceKm(new BigDecimal("50.0"))
                .status(RouteStatus.PLANNED)
                .createdAt(Instant.parse("2026-10-05T10:00:00Z"))
                .updatedAt(Instant.parse("2026-10-05T10:00:00Z"))
                .isNew(false)
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(availableVan);
        when(driverClient.getDriver(driverId)).thenReturn(activeDriver);
        when(repository.findActiveOverlappingCandidates(any(), any(), any()))
                .thenReturn(List.of(overlappingRoute));

        assertThatThrownBy(() -> routeService.create(createRequest, userId))
                .isInstanceOf(RouteOverlapException.class)
                .hasMessageContaining("solapa");
    }

    @Test
    @DisplayName("create should not throw overlap when existing route ends exactly when new route starts")
    void create_should_notThrowOverlap_when_existingRouteFinishesAtNewStart() {
        RouteEntity backToBackRoute = RouteEntity.builder()
                .id(UUID.randomUUID())
                .vehicleId(UUID.randomUUID())
                .driverId(driverId)
                .origin("Otro origen")
                .destination("Otro destino")
                .plannedStart(Instant.parse("2026-10-06T03:00:00Z"))
                .estimatedDurationMin(240)
                .plannedDistanceKm(new BigDecimal("50.0"))
                .status(RouteStatus.PLANNED)
                .createdAt(Instant.parse("2026-10-05T10:00:00Z"))
                .updatedAt(Instant.parse("2026-10-05T10:00:00Z"))
                .isNew(false)
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(availableVan);
        when(driverClient.getDriver(driverId)).thenReturn(activeDriver);
        when(repository.findActiveOverlappingCandidates(any(), any(), any()))
                .thenReturn(List.of(backToBackRoute));
        when(mapper.toEntity(any(RouteCreateRequest.class))).thenReturn(buildRouteEntity(RouteStatus.PLANNED));
        when(repository.save(any(RouteEntity.class))).thenReturn(buildRouteEntity(RouteStatus.PLANNED));
        when(mapper.toResponse(any(RouteEntity.class))).thenReturn(buildRouteResponse(RouteStatus.PLANNED));

        assertThatCode(() -> routeService.create(createRequest, userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("create should not throw overlap when candidates list is empty (different vehicle and driver)")
    void create_should_notThrowOverlap_when_candidateListEmpty() {
        when(vehicleClient.getVehicle(vehicleId)).thenReturn(availableVan);
        when(driverClient.getDriver(driverId)).thenReturn(activeDriver);
        when(repository.findActiveOverlappingCandidates(any(), any(), any())).thenReturn(List.of());
        when(mapper.toEntity(any(RouteCreateRequest.class))).thenReturn(buildRouteEntity(RouteStatus.PLANNED));
        when(repository.save(any(RouteEntity.class))).thenReturn(buildRouteEntity(RouteStatus.PLANNED));
        when(mapper.toResponse(any(RouteEntity.class))).thenReturn(buildRouteResponse(RouteStatus.PLANNED));

        assertThatCode(() -> routeService.create(createRequest, userId))
                .doesNotThrowAnyException();
    }

    private RouteEntity buildRouteEntity(RouteStatus status) {
        return RouteEntity.builder()
                .id(routeId)
                .vehicleId(vehicleId)
                .driverId(driverId)
                .origin("Madrid Centro Logístico")
                .destination("Valencia Puerto")
                .plannedStart(Instant.parse("2026-10-06T07:00:00Z"))
                .estimatedDurationMin(240)
                .plannedDistanceKm(new BigDecimal("355.0"))
                .status(status)
                .createdAt(Instant.parse("2026-10-05T10:00:00Z"))
                .updatedAt(Instant.parse("2026-10-05T10:00:00Z"))
                .isNew(false)
                .build();
    }

    private RouteResponse buildRouteResponse(RouteStatus status) {
        return RouteResponse.builder()
                .id(routeId)
                .vehicleId(vehicleId)
                .driverId(driverId)
                .origin("Madrid Centro Logístico")
                .destination("Valencia Puerto")
                .plannedStart(Instant.parse("2026-10-06T07:00:00Z"))
                .estimatedDurationMin(240)
                .plannedDistanceKm(new BigDecimal("355.0"))
                .status(status)
                .createdAt(Instant.parse("2026-10-05T10:00:00Z"))
                .updatedAt(Instant.parse("2026-10-05T10:00:00Z"))
                .build();
    }
}