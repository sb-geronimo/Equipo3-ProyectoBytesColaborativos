package es.bytescolab.msroutes.service;

import es.bytescolab.msroutes.client.DriverClient;
import es.bytescolab.msroutes.client.VehicleClient;
import es.bytescolab.msroutes.dto.request.RouteCreateRequest;
import es.bytescolab.msroutes.dto.response.ErrorResponse;
import es.bytescolab.msroutes.dto.response.VehicleDetailResponse;
import es.bytescolab.msroutes.enums.VehicleStatus;
import es.bytescolab.msroutes.enums.VehicleType;
import es.bytescolab.msroutes.exception.RouteExceptionHandler;
import es.bytescolab.msroutes.exception.RouteServiceUnavailableException;
import es.bytescolab.msroutes.mapper.RouteMapper;
import es.bytescolab.msroutes.repository.RouteRepository;
import es.bytescolab.msroutes.service.impl.RouteServiceImpl;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteResilienceTest {

    @Mock
    private RouteMapper mapper;

    @Mock
    private DriverClient driverClient;

    @Mock
    private VehicleClient vehicleClient;

    @Mock
    private RouteRepository repository;

    @InjectMocks
    private RouteServiceImpl service;

    private final RouteExceptionHandler exceptionHandler = new RouteExceptionHandler();

    @Test
    void msVehiclesCaido_planificarDevuelveServiceUnavailableConServicio() {
        UUID vehicleId = UUID.randomUUID();
        when(vehicleClient.getVehicle(vehicleId)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> service.create(routeCreateRequest(vehicleId), UUID.randomUUID()))
                .isInstanceOf(RouteServiceUnavailableException.class)
                .satisfies(thrownFailure -> {
                    RouteServiceUnavailableException unavailableFailure =
                            (RouteServiceUnavailableException) thrownFailure;
                    assertThat(unavailableFailure.getErrorCode()).isEqualTo("SERVICE_UNAVAILABLE");
                    assertThat(unavailableFailure.getService()).isEqualTo("ms-vehicles");
                    assertThat(unavailableFailure.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void circuitoAbierto_planificarDevuelveServiceUnavailableConServicio() {
        UUID vehicleId = UUID.randomUUID();
        when(vehicleClient.getVehicle(vehicleId))
                .thenThrow(mock(CallNotPermittedException.class));

        assertThatThrownBy(() -> service.create(routeCreateRequest(vehicleId), UUID.randomUUID()))
                .isInstanceOf(RouteServiceUnavailableException.class)
                .satisfies(thrownFailure -> {
                    RouteServiceUnavailableException unavailableFailure =
                            (RouteServiceUnavailableException) thrownFailure;
                    assertThat(unavailableFailure.getErrorCode()).isEqualTo("SERVICE_UNAVAILABLE");
                    assertThat(unavailableFailure.getService()).isEqualTo("ms-vehicles");
                });
    }

    @Test
    void msDriversCaido_planificarDevuelveServiceUnavailableConServicio() {
        UUID driverId = UUID.randomUUID();
        when(vehicleClient.getVehicle(any())).thenReturn(availableVehicle());
        when(driverClient.getDriver(any())).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> service.create(routeCreateRequestWithDriver(driverId), UUID.randomUUID()))
                .isInstanceOf(RouteServiceUnavailableException.class)
                .satisfies(thrownFailure -> {
                    RouteServiceUnavailableException unavailableFailure =
                            (RouteServiceUnavailableException) thrownFailure;
                    assertThat(unavailableFailure.getErrorCode()).isEqualTo("SERVICE_UNAVAILABLE");
                    assertThat(unavailableFailure.getService()).isEqualTo("ms-drivers");
                });
    }

    @Test
    void dependenciaCaida_respuestaIncluyeServicio() {
        RouteServiceUnavailableException unavailableFailure =
                new RouteServiceUnavailableException("ms-vehicles", "ms-vehicles no está disponible");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusiness(unavailableFailure);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(response.getBody().service()).isEqualTo("ms-vehicles");
    }

    private RouteCreateRequest routeCreateRequest(UUID vehicleId) {
        return new RouteCreateRequest(
                vehicleId,
                UUID.randomUUID(),
                "Madrid - Centro Logístico",
                "Valencia - Puerto",
                Instant.parse("2026-10-06T07:00:00Z"),
                240,
                new BigDecimal("355.0")
        );
    }

    private RouteCreateRequest routeCreateRequestWithDriver(UUID driverId) {
        return new RouteCreateRequest(
                UUID.randomUUID(),
                driverId,
                "Madrid - Centro Logístico",
                "Valencia - Puerto",
                Instant.parse("2026-10-06T07:00:00Z"),
                240,
                new BigDecimal("355.0")
        );
    }

    private VehicleDetailResponse availableVehicle() {
        return VehicleDetailResponse.builder()
                .id(UUID.randomUUID())
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
    }
}
