package es.bytescolab.msfuel.service.impl;

import es.bytescolab.msfuel.client.VehicleClient;
import es.bytescolab.msfuel.dto.request.RefuelCreateRequest;
import es.bytescolab.msfuel.dto.response.RefuelResponse;
import es.bytescolab.msfuel.dto.response.VehicleDetailResponse;
import es.bytescolab.msfuel.entity.FuelRecordEntity;
import es.bytescolab.msfuel.enums.FuelType;
import es.bytescolab.msfuel.enums.VehicleType;
import es.bytescolab.msfuel.exception.InvalidOdometerException;
import es.bytescolab.msfuel.exception.LitersExceedTankCapacityException;
import es.bytescolab.msfuel.exception.VehicleNotFoundException;
import es.bytescolab.msfuel.exception.VehiclesServiceUnavailableException;
import es.bytescolab.msfuel.mapper.FuelRecordMapper;
import es.bytescolab.msfuel.repository.FuelRecordRepository;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelRecordServiceImplTest {

    @Mock
    private FuelRecordRepository fuelRecordRepository;

    @Mock
    private VehicleClient vehicleClient;

    @Mock
    private FuelRecordMapper fuelRecordMapper;

    @InjectMocks
    private FuelRecordServiceImpl fuelRecordService;

    private UUID vehicleId;
    private VehicleDetailResponse vehicle;
    private RefuelCreateRequest request;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01");
        vehicle = VehicleDetailResponse.builder()
                .id(vehicleId)
                .type(VehicleType.VAN)
                .fuelType(FuelType.DIESEL)
                .tankCapacityL(80)
                .odometerKm(45000)
                .build();
        request = new RefuelCreateRequest(
                vehicleId,
                Instant.parse("2026-10-06T12:15:00Z"),
                new BigDecimal("50.00"),
                new BigDecimal("1.62"),
                45572,
                true,
                "Repsol A-3 km 112"
        );
    }

    @Test
    void create_should_returnRefuelResponse_when_happyPath() {
        FuelRecordEntity entity = FuelRecordEntity.builder().build();
        FuelRecordEntity saved = FuelRecordEntity.builder().build();
        RefuelResponse expected = RefuelResponse.builder().id(UUID.randomUUID()).build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicle);
        when(fuelRecordRepository.findPreviousRefuelBefore(eq(vehicleId), eq(request.refueledAt())))
                .thenReturn(Optional.empty());
        when(fuelRecordMapper.toEntity(request)).thenReturn(entity);
        when(fuelRecordRepository.save(any(FuelRecordEntity.class))).thenReturn(saved);
        when(fuelRecordMapper.toResponse(saved)).thenReturn(expected);

        RefuelResponse result = fuelRecordService.create(request);

        assertThat(result).isSameAs(expected);
        verify(fuelRecordRepository).save(entity);
    }

    @Test
    void create_should_throwLitersExceedTankCapacity_when_litersGreaterThanTank() {
        VehicleDetailResponse smallTankVehicle = VehicleDetailResponse.builder()
                .id(vehicleId)
                .type(VehicleType.VAN)
                .fuelType(FuelType.GASOLINE)
                .tankCapacityL(30)
                .odometerKm(45000)
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(smallTankVehicle);

        assertThatThrownBy(() -> fuelRecordService.create(request))
                .isInstanceOf(LitersExceedTankCapacityException.class);
    }

    @Test
    void create_should_throwInvalidOdometer_when_odometerNotGreaterThanPrevious() {
        Instant current = Instant.parse("2026-10-08T12:00:00Z");
        RefuelCreateRequest lowerOdometerRequest = new RefuelCreateRequest(
                vehicleId,
                current,
                new BigDecimal("50.00"),
                new BigDecimal("1.62"),
                45000,
                true,
                "Repsol A-3 km 112"
        );
        FuelRecordEntity previous = FuelRecordEntity.builder()
                .vehicleId(vehicleId)
                .refueledAt(Instant.parse("2026-10-01T12:00:00Z"))
                .odometerKm(45500)
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicle);
        when(fuelRecordRepository.findPreviousRefuelBefore(eq(vehicleId), eq(current)))
                .thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> fuelRecordService.create(lowerOdometerRequest))
                .isInstanceOf(InvalidOdometerException.class);
    }

    @Test
    void create_should_storeComputedConsumption_when_fullTankAndPreviousFullTankExists() {
        Instant current = Instant.parse("2026-10-08T12:00:00Z");
        Instant previousTime = Instant.parse("2026-10-01T12:00:00Z");
        RefuelCreateRequest fullTankRequest = new RefuelCreateRequest(
                vehicleId,
                current,
                new BigDecimal("50.00"),
                new BigDecimal("1.62"),
                50000,
                true,
                "Repsol A-3 km 112"
        );
        FuelRecordEntity entity = FuelRecordEntity.builder().build();
        FuelRecordEntity previousFullTank = FuelRecordEntity.builder()
                .vehicleId(vehicleId)
                .refueledAt(previousTime)
                .odometerKm(45000)
                .liters(new BigDecimal("50.00"))
                .build();
        FuelRecordEntity intermediate = FuelRecordEntity.builder()
                .vehicleId(vehicleId)
                .refueledAt(Instant.parse("2026-10-04T12:00:00Z"))
                .liters(new BigDecimal("10.00"))
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicle);
        when(fuelRecordRepository.findPreviousRefuelBefore(eq(vehicleId), eq(current)))
                .thenReturn(Optional.empty());
        when(fuelRecordMapper.toEntity(fullTankRequest)).thenReturn(entity);
        when(fuelRecordRepository.findLastFullTankAtOrBefore(eq(vehicleId), eq(current)))
                .thenReturn(Optional.of(previousFullTank));
        when(fuelRecordRepository.findRefuelsStrictlyBetween(eq(vehicleId), eq(previousTime), eq(current)))
                .thenReturn(List.of(intermediate));
        when(fuelRecordRepository.save(any(FuelRecordEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fuelRecordMapper.toResponse(any(FuelRecordEntity.class))).thenReturn(RefuelResponse.builder().build());

        fuelRecordService.create(fullTankRequest);

        ArgumentCaptor<FuelRecordEntity> captor = ArgumentCaptor.forClass(FuelRecordEntity.class);
        verify(fuelRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getConsumptionL100km())
                .isEqualByComparingTo(new BigDecimal("0.20"));
    }

    @Test
    void create_should_throwVehicleNotFound_when_vehicleClientReturnsNotFound() {
        when(vehicleClient.getVehicle(vehicleId)).thenThrow(notFoundFeignException());

        assertThatThrownBy(() -> fuelRecordService.create(request))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void create_should_throwVehiclesServiceUnavailable_when_vehicleClientReturnsServerError() {
        when(vehicleClient.getVehicle(vehicleId)).thenThrow(internalServerErrorFeignException());

        assertThatThrownBy(() -> fuelRecordService.create(request))
                .isInstanceOf(VehiclesServiceUnavailableException.class);
    }

    private static FeignException.NotFound notFoundFeignException() {
        return new FeignException.NotFound(
                "vehicle not found",
                Request.create(Request.HttpMethod.GET, "/api/vehicles", Collections.emptyMap(), null, null, null),
                new byte[0],
                Collections.emptyMap()
        );
    }

    private static FeignException internalServerErrorFeignException() {
        return new FeignException.InternalServerError(
                "vehicle service error",
                Request.create(Request.HttpMethod.GET, "/api/vehicles", Collections.emptyMap(), null, null, null),
                new byte[0],
                Collections.emptyMap()
        );
    }
}