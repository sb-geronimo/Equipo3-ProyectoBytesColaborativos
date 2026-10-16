package es.bytescolab.msvehicles.service.impl;

import es.bytescolab.msvehicles.dto.request.CreateVehicleRequest;
import es.bytescolab.msvehicles.dto.request.StatusVehicleRequest;
import es.bytescolab.msvehicles.dto.response.VehicleDetailsResponse;
import es.bytescolab.msvehicles.dto.response.VehicleSummaryResponse;
import es.bytescolab.msvehicles.entity.Vehicle;
import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.exception.InvalidOdometerException;
import es.bytescolab.msvehicles.exception.InvalidStatusTransitionException;
import es.bytescolab.msvehicles.exception.VehicleAlreadyExists;
import es.bytescolab.msvehicles.exception.VehicleNotFound;
import es.bytescolab.msvehicles.mapper.VehicleMapper;
import es.bytescolab.msvehicles.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private UUID vehicleId;
    private CreateVehicleRequest createRequest;
    private Vehicle entityToPersist;
    private Vehicle persistedVehicle;
    private VehicleDetailsResponse detailsResponse;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01");

        createRequest = new CreateVehicleRequest(
                "1234-ABC",
                "Toyota",
                "Corolla",
                2020,
                VehicleType.CAR,
                FuelType.GASOLINE,
                50,
                15000
        );

        entityToPersist = Vehicle.builder()
                .plate("1234-ABC")
                .make("Toyota")
                .model("Corolla")
                .year(2020)
                .type(VehicleType.CAR)
                .fuelType(FuelType.GASOLINE)
                .tankCapacityL(50)
                .odometerKm(15000)
                .status(VehicleStatus.AVAILABLE)
                .isNew(true)
                .build();

        persistedVehicle = Vehicle.builder()
                .id(vehicleId)
                .plate("1234-ABC")
                .make("Toyota")
                .model("Corolla")
                .year(2020)
                .type(VehicleType.CAR)
                .fuelType(FuelType.GASOLINE)
                .tankCapacityL(50)
                .odometerKm(15000)
                .status(VehicleStatus.AVAILABLE)
                .createdAt(Instant.parse("2026-10-05T10:30:00Z"))
                .updatedAt(Instant.parse("2026-10-05T10:30:00Z"))
                .isNew(false)
                .build();

        detailsResponse = new VehicleDetailsResponse(
                vehicleId,
                "1234-ABC",
                "Toyota",
                "Corolla",
                2020,
                VehicleType.CAR,
                FuelType.GASOLINE,
                50,
                15000,
                VehicleStatus.AVAILABLE,
                Instant.parse("2026-10-05T10:30:00Z"),
                Instant.parse("2026-10-05T10:30:00Z")
        );
    }

    @Test
    @DisplayName("createVehicle should return VehicleDetailsResponse when plate is unique")
    void createVehicle_should_returnDetailsResponse_when_plateIsUnique() {
        when(vehicleMapper.toEntity(createRequest)).thenReturn(entityToPersist);
        when(vehicleRepository.saveAndFlush(entityToPersist)).thenReturn(persistedVehicle);
        when(vehicleMapper.toDetailsResponse(persistedVehicle)).thenReturn(detailsResponse);

        VehicleDetailsResponse actual = vehicleService.createVehicle(createRequest);

        assertThat(actual).isEqualTo(detailsResponse);
        verify(vehicleMapper).toEntity(createRequest);
        verify(vehicleRepository).saveAndFlush(entityToPersist);
        verify(vehicleMapper).toDetailsResponse(persistedVehicle);
    }

    @Test
    @DisplayName("createVehicle should throw VehicleAlreadyExists when repository reports plate duplicate")
    void createVehicle_should_throwVehicleAlreadyExists_when_plateDuplicated() {
        when(vehicleMapper.toEntity(createRequest)).thenReturn(entityToPersist);
        when(vehicleRepository.saveAndFlush(entityToPersist))
                .thenThrow(new DataIntegrityViolationException("uk_vehicle_plate"));

        assertThatThrownBy(() -> vehicleService.createVehicle(createRequest))
                .isInstanceOf(VehicleAlreadyExists.class)
                .hasMessageContaining("1234-ABC");

        verify(vehicleMapper, never()).toDetailsResponse(any());
    }

    @Test
    @DisplayName("updateStatusVehicle should throw InvalidStatusTransitionException when transition is denied")
    void updateStatusVehicle_should_throwInvalidStatusTransition_when_transitionDenied() {
        Vehicle inUseVehicle = Vehicle.builder()
                .id(vehicleId)
                .plate("1234-ABC")
                .make("Toyota")
                .model("Corolla")
                .year(2020)
                .type(VehicleType.CAR)
                .fuelType(FuelType.GASOLINE)
                .tankCapacityL(50)
                .odometerKm(20000)
                .status(VehicleStatus.IN_USE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isNew(false)
                .build();
        StatusVehicleRequest request = new StatusVehicleRequest(VehicleStatus.IN_MAINTENANCE, null);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(inUseVehicle));

        assertThatThrownBy(() -> vehicleService.updateStatusVehicle(vehicleId, request))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("transición");

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatusVehicle should throw InvalidOdometerException when new odometer is lower than current")
    void updateStatusVehicle_should_throwInvalidOdometer_when_odometerRegresses() {
        Vehicle storedVehicle = Vehicle.builder()
                .id(vehicleId)
                .plate("1234-ABC")
                .make("Toyota")
                .model("Corolla")
                .year(2020)
                .type(VehicleType.CAR)
                .fuelType(FuelType.GASOLINE)
                .tankCapacityL(50)
                .odometerKm(50000)
                .status(VehicleStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isNew(false)
                .build();
        StatusVehicleRequest request = new StatusVehicleRequest(VehicleStatus.IN_USE, 30000);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(storedVehicle));

        assertThatThrownBy(() -> vehicleService.updateStatusVehicle(vehicleId, request))
                .isInstanceOf(InvalidOdometerException.class)
                .hasMessageContaining("odómetro");

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatusVehicle should persist new status and return summary when transition is allowed")
    void updateStatusVehicle_should_returnSummary_when_transitionIsAllowed() {
        Vehicle storedVehicle = Vehicle.builder()
                .id(vehicleId)
                .plate("1234-ABC")
                .make("Toyota")
                .model("Corolla")
                .year(2020)
                .type(VehicleType.CAR)
                .fuelType(FuelType.GASOLINE)
                .tankCapacityL(50)
                .odometerKm(10000)
                .status(VehicleStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isNew(false)
                .build();
        Vehicle updatedVehicle = Vehicle.builder()
                .id(vehicleId)
                .plate("1234-ABC")
                .make("Toyota")
                .model("Corolla")
                .year(2020)
                .type(VehicleType.CAR)
                .fuelType(FuelType.GASOLINE)
                .tankCapacityL(50)
                .odometerKm(10000)
                .status(VehicleStatus.IN_USE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isNew(false)
                .build();
        StatusVehicleRequest request = new StatusVehicleRequest(VehicleStatus.IN_USE, null);
        VehicleSummaryResponse summaryResponse = new VehicleSummaryResponse(
                vehicleId,
                "1234-ABC",
                "Toyota",
                "Corolla",
                2020,
                VehicleType.CAR,
                FuelType.GASOLINE,
                50,
                10000,
                VehicleStatus.IN_USE
        );

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(storedVehicle));
        when(vehicleRepository.save(storedVehicle)).thenReturn(updatedVehicle);
        when(vehicleMapper.toSummaryResponse(updatedVehicle)).thenReturn(summaryResponse);

        VehicleSummaryResponse actual = vehicleService.updateStatusVehicle(vehicleId, request);

        assertThat(actual).isEqualTo(summaryResponse);
        assertThat(storedVehicle.getStatus()).isEqualTo(VehicleStatus.IN_USE);
        verify(vehicleRepository).save(storedVehicle);
        verify(vehicleMapper).toSummaryResponse(updatedVehicle);
    }

    @Test
    @DisplayName("findVehicleById should throw VehicleNotFound when no vehicle matches the id")
    void findVehicleById_should_throwVehicleNotFound_when_vehicleMissing() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.findVehicleById(vehicleId))
                .isInstanceOf(VehicleNotFound.class)
                .hasMessageContaining("ID");

        verify(vehicleMapper, never()).toDetailsResponse(any());
    }
}
