package es.bytescolab.msroutes.service.impl;

import es.bytescolab.msroutes.client.DriverFeignClient;
import es.bytescolab.msroutes.client.VehicleFeignClient;
import es.bytescolab.msroutes.client.dto.DriverFeignResponse;
import es.bytescolab.msroutes.client.dto.VehicleFeignResponse;
import es.bytescolab.msroutes.dto.request.CreateRouteRequest;
import es.bytescolab.msroutes.dto.response.RouteDetailResponse;
import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.RouteStatus;
import es.bytescolab.msroutes.exception.DriverNotEligibleException;
import es.bytescolab.msroutes.exception.DriverNotFoundException;
import es.bytescolab.msroutes.exception.RouteNotFoundException;
import es.bytescolab.msroutes.exception.RouteOverlapException;
import es.bytescolab.msroutes.exception.ServiceUnavailableException;
import es.bytescolab.msroutes.exception.VehicleNotAvailableException;
import es.bytescolab.msroutes.exception.VehicleNotFoundException;
import es.bytescolab.msroutes.mapper.RouteMapper;
import es.bytescolab.msroutes.repository.RouteRepository;
import es.bytescolab.msroutes.service.RouteAssignmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private RouteMapper routeMapper;
    @Mock
    private VehicleFeignClient vehicleFeignClient;
    @Mock
    private DriverFeignClient driverFeignClient;

    private RouteAssignmentValidator validator;
    private RouteServiceImpl service;

    private final UUID vehicleId = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01");
    private final UUID driverId = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01");
    private final UUID createdById = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private final Instant plannedStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

    @BeforeEach
    void setUp() {
        validator = new RouteAssignmentValidator();
        service = new RouteServiceImpl(routeRepository, routeMapper, vehicleFeignClient,
                driverFeignClient, validator);
    }

    private CreateRouteRequest buildRequest() {
        return new CreateRouteRequest(
                vehicleId,
                driverId,
                "Madrid - Centro Logistico",
                "Valencia - Puerto",
                plannedStart,
                240,
                new BigDecimal("355.00")
        );
    }

    private VehicleFeignResponse availableVehicle() {
        return new VehicleFeignResponse(vehicleId, "AVAILABLE", "CAR");
    }

    private DriverFeignResponse activeDriver() {
        return new DriverFeignResponse(driverId, "ACTIVE", "B", LocalDate.now().plusDays(120));
    }

    @Test
    @DisplayName("create devuelve detalle con status PLANNED en el happy path")
    void createHappyPath() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId)).thenReturn(availableVehicle());
        when(driverFeignClient.findById(driverId)).thenReturn(activeDriver());
        when(routeRepository.findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
                any(), any(), any(), any())).thenReturn(List.of());

        RouteEntity mapped = RouteEntity.builder().id(UUID.randomUUID()).build();
        when(routeMapper.toEntity(request)).thenReturn(mapped);

        RouteEntity saved = RouteEntity.builder()
                .id(mapped.getId())
                .vehicleId(vehicleId)
                .driverId(driverId)
                .status(RouteStatus.PLANNED)
                .build();
        when(routeRepository.save(mapped)).thenReturn(saved);

        RouteDetailResponse response = new RouteDetailResponse(
                saved.getId(), vehicleId, driverId, "Madrid - Centro Logistico",
                "Valencia - Puerto", plannedStart, 240, new BigDecimal("355.00"),
                RouteStatus.PLANNED, null, null, null, null, null, null,
                createdById, Instant.now(), Instant.now());
        when(routeMapper.toDetailResponse(saved)).thenReturn(response);

        RouteDetailResponse actual = service.create(request);

        assertNotNull(actual);
        assertEquals(RouteStatus.PLANNED, actual.status());
        assertSame(response, actual);
    }

    @Test
    @DisplayName("create propaga VehicleNotFoundException del Feign y no llama al driver")
    void createPropagatesVehicleNotFound() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId))
                .thenThrow(new VehicleNotFoundException());

        assertThrows(VehicleNotFoundException.class, () -> service.create(request));

        verify(driverFeignClient, never()).findById(any(UUID.class));
        verify(routeRepository, never()).save(any(RouteEntity.class));
    }

    @Test
    @DisplayName("create lanza ServiceUnavailableException cuando ms-vehicles responde 5xx")
    void createTranslatesVehicle5xx() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId))
                .thenThrow(new ServiceUnavailableException("ms-vehicles", "HTTP 503"));

        assertThrows(ServiceUnavailableException.class, () -> service.create(request));

        verify(driverFeignClient, never()).findById(any(UUID.class));
        verify(routeRepository, never()).save(any(RouteEntity.class));
    }

    @Test
    @DisplayName("create propaga VehicleNotAvailableException si el vehiculo esta OUT_OF_SERVICE")
    void createRejectsOutOfServiceVehicle() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId))
                .thenReturn(new VehicleFeignResponse(vehicleId, "OUT_OF_SERVICE", "CAR"));

        assertThrows(VehicleNotAvailableException.class, () -> service.create(request));

        verify(driverFeignClient, never()).findById(any(UUID.class));
        verify(routeRepository, never()).save(any(RouteEntity.class));
    }

    @Test
    @DisplayName("create propaga DriverNotFoundException sin guardar la ruta")
    void createPropagatesDriverNotFound() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId)).thenReturn(availableVehicle());
        when(driverFeignClient.findById(driverId))
                .thenThrow(new DriverNotFoundException());

        assertThrows(DriverNotFoundException.class, () -> service.create(request));

        verify(routeRepository, never()).save(any(RouteEntity.class));
    }

    @Test
    @DisplayName("create propaga DriverNotEligibleException cuando el driver no esta activo")
    void createRejectsInactiveDriver() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId)).thenReturn(availableVehicle());
        when(driverFeignClient.findById(driverId))
                .thenReturn(new DriverFeignResponse(driverId, "ON_LEAVE", "B", LocalDate.now().plusDays(120)));

        assertThrows(DriverNotEligibleException.class, () -> service.create(request));

        verify(routeRepository, never()).save(any(RouteEntity.class));
    }

    @Test
    @DisplayName("create lanza ServiceUnavailableException cuando ms-drivers responde 5xx")
    void createTranslatesDriver5xx() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId)).thenReturn(availableVehicle());
        when(driverFeignClient.findById(driverId))
                .thenThrow(new ServiceUnavailableException("ms-drivers", "HTTP 500"));

        assertThrows(ServiceUnavailableException.class, () -> service.create(request));

        verify(routeRepository, never()).save(any(RouteEntity.class));
    }

    @Test
    @DisplayName("create lanza RouteOverlapException cuando existe solapamiento")
    void createRejectsOverlap() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        RouteEntity conflict = RouteEntity.builder()
                .id(UUID.randomUUID())
                .vehicleId(vehicleId)
                .driverId(driverId)
                .plannedStart(plannedStart.plus(Duration.ofMinutes(30)))
                .estimatedDurationMin(120)
                .status(RouteStatus.PLANNED)
                .build();

        when(vehicleFeignClient.findById(vehicleId)).thenReturn(availableVehicle());
        when(driverFeignClient.findById(driverId)).thenReturn(activeDriver());
        when(routeRepository.findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
                any(), any(), any(), any()))
                .thenReturn(List.of(conflict));

        assertThrows(RouteOverlapException.class, () -> service.create(request));

        verify(routeRepository, never()).save(any(RouteEntity.class));
    }

    @Test
    @DisplayName("create asigna createdBy desde el principal y delega en mapper")
    void createSetsCreatedBy() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId)).thenReturn(availableVehicle());
        when(driverFeignClient.findById(driverId)).thenReturn(activeDriver());
        when(routeRepository.findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
                any(), any(), any(), any())).thenReturn(List.of());

        RouteEntity mapped = RouteEntity.builder().id(UUID.randomUUID()).build();
        when(routeMapper.toEntity(request)).thenReturn(mapped);

        RouteEntity saved = RouteEntity.builder().id(mapped.getId())
                .vehicleId(vehicleId).driverId(driverId).status(RouteStatus.PLANNED).build();
        when(routeRepository.save(mapped)).thenReturn(saved);
        when(routeMapper.toDetailResponse(saved)).thenReturn(new RouteDetailResponse(
                saved.getId(), vehicleId, driverId, "Madrid", "Valencia", plannedStart,
                240, new BigDecimal("355.00"), RouteStatus.PLANNED, null, null, null, null,
                null, null, createdById, Instant.now(), Instant.now()));

        service.create(request);

        ArgumentCaptor<RouteEntity> captor = ArgumentCaptor.forClass(RouteEntity.class);
        verify(routeRepository).save(captor.capture());
        assertEquals(createdById, captor.getValue().getCreatedBy());
    }

    @Test
    @DisplayName("create guarda createdBy=null cuando el principal es internal")
    void createSetsNullCreatedByForInternalPrincipal() {
        authenticateAs("internal");
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId)).thenReturn(availableVehicle());
        when(driverFeignClient.findById(driverId)).thenReturn(activeDriver());
        when(routeRepository.findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
                any(), any(), any(), any())).thenReturn(List.of());

        RouteEntity mapped = RouteEntity.builder().id(UUID.randomUUID()).build();
        when(routeMapper.toEntity(request)).thenReturn(mapped);

        RouteEntity saved = RouteEntity.builder().id(mapped.getId())
                .vehicleId(vehicleId).driverId(driverId).status(RouteStatus.PLANNED).build();
        when(routeRepository.save(mapped)).thenReturn(saved);
        when(routeMapper.toDetailResponse(saved)).thenReturn(new RouteDetailResponse(
                saved.getId(), vehicleId, driverId, "Madrid", "Valencia", plannedStart,
                240, new BigDecimal("355.00"), RouteStatus.PLANNED, null, null, null, null,
                null, null, null, Instant.now(), Instant.now()));

        service.create(request);

        ArgumentCaptor<RouteEntity> captor = ArgumentCaptor.forClass(RouteEntity.class);
        verify(routeRepository).save(captor.capture());
        assertNull(captor.getValue().getCreatedBy());
    }

    @Test
    @DisplayName("findById lanza RouteNotFoundException cuando el repositorio no encuentra la ruta")
    void findByIdNotFound() {
        when(routeRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.empty());
        assertThrows(RouteNotFoundException.class, () -> service.findById(UUID.randomUUID()));
    }

    @Test
    @DisplayName("create respeta el orden: vehiculo primero, luego driver, luego repository.save")
    void createOrderingRespected() {
        authenticateAs(createdById);
        CreateRouteRequest request = buildRequest();

        when(vehicleFeignClient.findById(vehicleId)).thenReturn(availableVehicle());
        when(driverFeignClient.findById(driverId)).thenReturn(activeDriver());
        when(routeRepository.findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
                any(), any(), any(), any())).thenReturn(List.of());

        RouteEntity mapped = RouteEntity.builder().id(UUID.randomUUID()).build();
        when(routeMapper.toEntity(request)).thenReturn(mapped);

        RouteEntity saved = RouteEntity.builder().id(mapped.getId())
                .vehicleId(vehicleId).driverId(driverId).status(RouteStatus.PLANNED).build();
        when(routeRepository.save(mapped)).thenReturn(saved);
        when(routeMapper.toDetailResponse(saved)).thenReturn(new RouteDetailResponse(
                saved.getId(), vehicleId, driverId, "Madrid", "Valencia", plannedStart,
                240, new BigDecimal("355.00"), RouteStatus.PLANNED, null, null, null, null,
                null, null, createdById, Instant.now(), Instant.now()));

        service.create(request);

        InOrder ordered = org.mockito.Mockito.inOrder(vehicleFeignClient, driverFeignClient, routeRepository);
        ordered.verify(vehicleFeignClient).findById(vehicleId);
        ordered.verify(driverFeignClient).findById(driverId);
        ordered.verify(routeRepository).save(mapped);
    }

    private void authenticateAs(String principal) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateAs(UUID principal) {
        authenticateAs(principal.toString());
    }
}