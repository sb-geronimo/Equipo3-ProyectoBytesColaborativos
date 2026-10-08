package es.bytescolab.msroutes.controller;

import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import es.bytescolab.msroutes.dto.request.CreateRouteRequest;
import es.bytescolab.msroutes.security.InternalKeyFilter;
import es.bytescolab.msroutes.security.JwtAuthFilter;
import es.bytescolab.msroutes.security.JwtUtil;
import es.bytescolab.msroutes.security.TokenAccessDeniedHandler;
import es.bytescolab.msroutes.security.TokenAuthenticationEntryPoint;
import es.bytescolab.msroutes.dto.response.PageResponse;
import es.bytescolab.msroutes.dto.response.RouteDetailResponse;
import es.bytescolab.msroutes.dto.response.RouteSummaryResponse;
import es.bytescolab.msroutes.enums.RouteStatus;
import es.bytescolab.msroutes.exception.DriverNotEligibleException;
import es.bytescolab.msroutes.exception.DriverNotFoundException;
import es.bytescolab.msroutes.exception.RouteNotFoundException;
import es.bytescolab.msroutes.exception.RouteOverlapException;
import es.bytescolab.msroutes.exception.ServiceUnavailableException;
import es.bytescolab.msroutes.exception.VehicleNotAvailableException;
import es.bytescolab.msroutes.exception.VehicleNotFoundException;
import es.bytescolab.msroutes.service.RouteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(es.bytescolab.msroutes.exception.RouteExceptionHandler.class)
class RouteControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private InternalKeyFilter internalKeyFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint;

    @MockitoBean
    private TokenAccessDeniedHandler tokenAccessDeniedHandler;

    private final UUID vehicleId = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01");
    private final UUID driverId = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01");
    private final UUID routeId = UUID.fromString("c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201");
    private final Instant plannedStart = Instant.now().plus(2, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.SECONDS);

    private CreateRouteRequest validRequest() {
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

    @Test
    @DisplayName("POST /api/routes con origin en blanco devuelve 400 VALIDATION_ERROR")
    void postWithBlankOriginReturns400() throws Exception {
        CreateRouteRequest invalid = new CreateRouteRequest(
                vehicleId,
                driverId,
                "",
                "Valencia - Puerto",
                plannedStart,
                240,
                new BigDecimal("355.00")
        );
        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/routes con plannedStart en el pasado devuelve 400 VALIDATION_ERROR")
    void postWithPastPlannedStartReturns400() throws Exception {
        CreateRouteRequest invalid = new CreateRouteRequest(
                vehicleId,
                driverId,
                "Madrid",
                "Valencia",
                Instant.now().minus(1, ChronoUnit.DAYS),
                240,
                new BigDecimal("355.00")
        );
        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/routes devuelve 201 con detalle cuando el servicio responde OK")
    void createReturns201() throws Exception {
        RouteDetailResponse response = new RouteDetailResponse(
                routeId, vehicleId, driverId, "Madrid - Centro Logistico",
                "Valencia - Puerto", plannedStart, 240, new BigDecimal("355.00"),
                RouteStatus.PLANNED, null, null, null, null, null, null,
                null, Instant.now(), Instant.now());

        when(routeService.create(any(CreateRouteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(routeId.toString()))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    @DisplayName("POST /api/routes traduce VehicleNotFoundException a 404 VEHICLE_NOT_FOUND")
    void postVehicleNotFoundExceptionReturns404() throws Exception {
        when(routeService.create(any(CreateRouteRequest.class)))
                .thenThrow(new VehicleNotFoundException());

        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("VEHICLE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/routes traduce DriverNotFoundException a 404 DRIVER_NOT_FOUND")
    void postDriverNotFoundExceptionReturns404() throws Exception {
        when(routeService.create(any(CreateRouteRequest.class)))
                .thenThrow(new DriverNotFoundException());

        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DRIVER_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/routes traduce VehicleNotAvailableException a 409 VEHICLE_NOT_AVAILABLE")
    void postVehicleNotAvailableExceptionReturns409() throws Exception {
        when(routeService.create(any(CreateRouteRequest.class)))
                .thenThrow(new VehicleNotAvailableException());

        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("VEHICLE_NOT_AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/routes traduce DriverNotEligibleException a 409 DRIVER_NOT_ELIGIBLE")
    void postDriverNotEligibleExceptionReturns409() throws Exception {
        when(routeService.create(any(CreateRouteRequest.class)))
                .thenThrow(new DriverNotEligibleException("licencia caducada"));

        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DRIVER_NOT_ELIGIBLE"));
    }

    @Test
    @DisplayName("POST /api/routes traduce RouteOverlapException a 409 ROUTE_OVERLAP")
    void postRouteOverlapExceptionReturns409() throws Exception {
        when(routeService.create(any(CreateRouteRequest.class)))
                .thenThrow(new RouteOverlapException());

        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ROUTE_OVERLAP"));
    }

    @Test
    @DisplayName("POST /api/routes traduce ServiceUnavailableException a 503 con service name")
    void postServiceUnavailableExceptionReturns503() throws Exception {
        when(routeService.create(any(CreateRouteRequest.class)))
                .thenThrow(new ServiceUnavailableException("ms-vehicles", "HTTP 503"));

        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.service").value("ms-vehicles"));
    }

    @Test
    @DisplayName("GET /api/routes/{id} devuelve 200 con detalle cuando existe")
    void findByIdReturns200() throws Exception {
        RouteDetailResponse response = new RouteDetailResponse(
                routeId, vehicleId, driverId, "Madrid", "Valencia", plannedStart,
                240, new BigDecimal("355.00"), RouteStatus.PLANNED,
                null, null, null, null, null, null, null,
                Instant.now(), Instant.now());

        when(routeService.findById(routeId)).thenReturn(response);

        mockMvc.perform(get("/api/routes/{routeId}", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(routeId.toString()));
    }

    @Test
    @DisplayName("GET /api/routes/{id} devuelve 404 ROUTE_NOT_FOUND cuando no existe")
    void findByIdReturns404() throws Exception {
        when(routeService.findById(routeId)).thenThrow(new RouteNotFoundException());

        mockMvc.perform(get("/api/routes/{routeId}", routeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROUTE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/routes aplica defaults de paginacion (page=0 size=20)")
    void listAppliesDefaults() throws Exception {
        RouteSummaryResponse summary = new RouteSummaryResponse(
                routeId, vehicleId, driverId, plannedStart,
                new BigDecimal("355.00"), RouteStatus.PLANNED, Instant.now());
        PageResponse<RouteSummaryResponse> page = new PageResponse<>(
                List.of(summary), 0, 20, 1, 1);

        when(routeService.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(routeId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PLANNED"));
    }

    @Test
    @DisplayName("GET /api/routes?status=PLANNED&from=2026-10-01&to=2026-10-31 propaga los filtros")
    void listPassesFilters() throws Exception {
        when(routeService.findAll(eq(null), eq(null), eq(RouteStatus.PLANNED),
                eq(LocalDate.parse("2026-10-01")), eq(LocalDate.parse("2026-10-31")), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/routes")
                        .param("status", "PLANNED")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("POST /api/routes con body invalido responde 400 con mensaje en JSON")
    void postInvalidBodyReturns400() throws Exception {
        String invalidJson = """
                {
                  "vehicleId": null,
                  "driverId": null,
                  "origin": "",
                  "destination": "",
                  "plannedStart": null,
                  "estimatedDurationMin": 0,
                  "plannedDistanceKm": 0
                }
                """;
        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}