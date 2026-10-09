package es.bytescolab.msroutes.controller;

import es.bytescolab.msroutes.dto.request.RouteCompleteRequest;
import es.bytescolab.msroutes.dto.request.RouteCreateRequest;
import es.bytescolab.msroutes.dto.request.RouteFilterRequest;
import es.bytescolab.msroutes.dto.request.RouteStatsRequest;
import es.bytescolab.msroutes.dto.response.PageResponse;
import es.bytescolab.msroutes.dto.response.ErrorResponse;
import es.bytescolab.msroutes.dto.response.RouteResponse;
import es.bytescolab.msroutes.dto.response.RouteStatsResponse;
import es.bytescolab.msroutes.enums.RouteStatus;
import es.bytescolab.msroutes.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Routes", description = "Planificación, ejecución e historial de rutas")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class RouteController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_PAGE_SIZE = "20";

    private final RouteService routeService;

    @Operation(
            summary = "Planificar una nueva ruta",
            description = "Valida el vehículo y el conductor llamando a ms-vehicles y ms-drivers, "
                    + "comprueba las reglas de negocio y crea la ruta en estado PLANNED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ruta planificada correctamente",
                    content = @Content(schema = @Schema(implementation = RouteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "Vehículo o conductor inexistente (VEHICLE_NOT_FOUND / DRIVER_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "Vehículo no disponible, conductor no elegible o solapamiento de ruta "
                            + "(VEHICLE_NOT_AVAILABLE / DRIVER_NOT_ELIGIBLE / ROUTE_OVERLAP)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503",
                    description = "ms-vehicles o ms-drivers no disponibles (SERVICE_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RouteResponse> create(
            @Valid @RequestBody RouteCreateRequest request,
            @Parameter(description = "Identificador del usuario autenticado que planifica la ruta",
                    hidden = true)
            @AuthenticationPrincipal String userId
    ) {
        UUID creator = UUID.fromString(userId);
        log.info("Petición POST /api/routes recibida — vehicleId={}, driverId={}, plannedStart={}",
                request.vehicleId(), request.driverId(), request.plannedStart());

        RouteResponse response = routeService.create(request, creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Listar rutas paginadas",
            description = "Listado paginado de rutas ordenado por plannedStart descendente. "
                    + "Permite filtrar por vehículo, conductor, estado y rango de fechas sobre plannedStart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de rutas",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Parámetros de filtrado o paginación inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<RouteResponse>> findAll(
            @Parameter(description = "Filtra por identificador de vehículo",
                    example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61")
            @RequestParam(value = "vehicle", required = false) UUID vehicle,

            @Parameter(description = "Filtra por identificador de conductor",
                    example = "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01")
            @RequestParam(value = "driver", required = false) UUID driver,

            @Parameter(description = "Filtra por estado de la ruta",
                    example = "PLANNED", schema = @Schema(implementation = RouteStatus.class))
            @RequestParam(value = "status", required = false) RouteStatus status,

            @Parameter(description = "Fecha inicial (inclusiva) del filtro sobre plannedStart",
                    example = "2026-10-01")
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Fecha final (inclusiva) del filtro sobre plannedStart",
                    example = "2026-10-31")
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Número de página (0 en adelante)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página (por defecto 20, máximo 100)", example = "20")
            @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) int size
    ) {
        RouteFilterRequest filter = new RouteFilterRequest(
                vehicle, driver, status, from, to, page, size
        );
        log.info("Petición GET /api/routes recibida — vehicle={}, driver={}, status={}, "
                        + "from={}, to={}, page={}, size={}",
                filter.vehicle(), filter.driver(), filter.status(),
                filter.from(), filter.to(), filter.page(), filter.size());

        int safePage = filter.page();
        int safeSize = Math.min(Math.max(filter.size(), 1), MAX_PAGE_SIZE);
        Page<RouteResponse> pageResult = routeService.findAll(
                filter.vehicle(), filter.driver(), filter.status(),
                filter.from(), filter.to(), safePage, safeSize
        );
        return ResponseEntity.ok(PageResponse.of(pageResult));
    }

    @Operation(
            summary = "Detalle de una ruta",
            description = "Devuelve la información completa de la ruta, incluidos los marcadores de auditoría."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ruta encontrada",
                    content = @Content(schema = @Schema(implementation = RouteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ruta no encontrada (ROUTE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{routeId}")
    public ResponseEntity<RouteResponse> findById(
            @Parameter(description = "Identificador de la ruta",
                    example = "5b9e2c10-7f4a-4d55-b1a0-3c8e6f7a9201")
            @PathVariable UUID routeId
    ) {
        log.info("Petición GET /api/routes/{} recibida", routeId);
        RouteResponse response = routeService.findById(routeId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Iniciar una ruta planificada",
            description = "Marca la ruta como IN_PROGRESS y cambia el vehículo a IN_USE en ms-vehicles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Ruta iniciada, vehículo en IN_USE",
                    content = @Content(schema = @Schema(implementation = RouteResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "Estado de la ruta no válido o vehículo no disponible "
                            + "(INVALID_ROUTE_STATE / VEHICLE_NOT_AVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503",
                    description = "ms-vehicles no disponible (SERVICE_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{routeId}/start")
    public ResponseEntity<RouteResponse> start(
            @Parameter(description = "Identificador de la ruta a iniciar",
                    example = "5b9e2c10-7f4a-4d55-b1a0-3c8e6f7a9201")
            @PathVariable UUID routeId
    ) {
        log.info("Petición POST /api/routes/{}/start recibida", routeId);
        RouteResponse response = routeService.start(routeId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Completar una ruta en curso",
            description = "Calcula endOdometerKm sumando actualDistanceKm redondeada a startOdometerKm "
                    + "y devuelve el vehículo a AVAILABLE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ruta completada",
                    content = @Content(schema = @Schema(implementation = RouteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "Estado de la ruta no válido (INVALID_ROUTE_STATE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503",
                    description = "ms-vehicles no disponible (SERVICE_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{routeId}/complete")
    public ResponseEntity<RouteResponse> complete(
            @Parameter(description = "Identificador de la ruta a completar",
                    example = "5b9e2c10-7f4a-4d55-b1a0-3c8e6f7a9201")
            @PathVariable UUID routeId,

            @Valid @RequestBody RouteCompleteRequest request
    ) {
        log.info("Petición POST /api/routes/{}/complete recibida — actualDistanceKm={}",
                routeId, request.actualDistanceKm());
        RouteResponse response = routeService.complete(routeId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Serie temporal de rutas completadas",
            description = "Lo consume ms-dashboard. Agrupa por fecha de finalización con "
                    + "granularidad DAY, WEEK o MONTH."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serie temporal generada",
                    content = @Content(schema = @Schema(implementation = RouteStatsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<RouteStatsResponse> getStats(
            @Parameter(description = "Fecha inicial del rango (inclusiva)", example = "2026-10-01")
            @RequestParam("from") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Fecha final del rango (inclusiva)", example = "2026-10-31")
            @RequestParam("to") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Granularidad de la serie temporal",
                    example = "DAY", schema = @Schema(allowableValues = {"DAY", "WEEK", "MONTH"}))
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY")
            String granularity,

            @Parameter(description = "Filtra la serie por identificador de vehículo",
                    example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61")
            @RequestParam(value = "vehicle", required = false) UUID vehicle
    ) {
        log.info("Petición GET /api/routes/stats recibida — from={}, to={}, granularity={}, vehicle={}",
                from, to, granularity, vehicle);

        RouteStatsRequest request = new RouteStatsRequest(from, to, granularity, vehicle);
        RouteStatsResponse response = routeService.getStats(request);
        return ResponseEntity.ok(response);
    }
}
