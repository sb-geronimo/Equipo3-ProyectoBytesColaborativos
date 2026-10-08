package es.bytescolab.msroutes.controller;

import es.bytescolab.msroutes.dto.request.CreateRouteRequest;
import es.bytescolab.msroutes.dto.response.ErrorResponse;
import es.bytescolab.msroutes.dto.response.PageResponse;
import es.bytescolab.msroutes.dto.response.RouteDetailResponse;
import es.bytescolab.msroutes.dto.response.RouteSummaryResponse;
import es.bytescolab.msroutes.enums.RouteStatus;
import es.bytescolab.msroutes.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Tag(name = "Rutas",
        description = "Planificacion, ejecucion e historial de rutas de la flota")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class RouteController {

    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final int MAX_PAGE_SIZE = 100;

    private final RouteService routeService;

    @Operation(
            summary = "Listar rutas",
            description = "Lista paginada de rutas, ordenada por plannedStart descendente. "
                    + "Filtros opcionales por vehiculo, conductor, estado y ventana de fechas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de rutas",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<RouteSummaryResponse>> findAll(
            @Parameter(description = "Filtro por vehiculo asignado",
                    example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
            @RequestParam(value = "vehicle", required = false) UUID vehicle,

            @Parameter(description = "Filtro por conductor asignado",
                    example = "3b2d8f10-7c4a-4e55-bla0-5d9e2c7f1a01")
            @RequestParam(value = "driver", required = false) UUID driver,

            @Parameter(description = "Filtro por estado de la ruta", example = "PLANNED")
            @RequestParam(value = "status", required = false) RouteStatus status,

            @Parameter(description = "Fecha de inicio (inclusiva) del filtro por plannedStart",
                    example = "2026-10-01")
            @RequestParam(value = "from", required = false) LocalDate from,

            @Parameter(description = "Fecha de fin (inclusiva) del filtro por plannedStart",
                    example = "2026-10-31")
            @RequestParam(value = "to", required = false) LocalDate to,

            @Parameter(description = "Numero de pagina (0 en adelante)", example = "0")
            @RequestParam(value = "page", defaultValue = "0")
            int page,

            @Parameter(description = "Tamano de pagina (por defecto 20, maximo " + MAX_PAGE_SIZE + ")",
                    example = "20")
            @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE)
            int size
    ) {
        Pageable effectivePageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "plannedStart"));
        log.debug("List routes — vehicle={}, driver={}, status={}, from={}, to={}, page={}, size={}",
                vehicle, driver, status, from, to, page, size);
        var response = routeService.findAll(vehicle, driver, status, from, to, effectivePageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Detalle de una ruta",
            description = "Devuelve el detalle completo de la ruta, incluidos los campos de ejecucion "
                    + "(startedAt, endedAt, startOdometerKm, endOdometerKm, actualDistanceKm, notes)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ruta encontrada",
                    content = @Content(schema = @Schema(implementation = RouteDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ruta no encontrada (ROUTE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "No existe", value = """
                                    {
                                      "error": "ROUTE_NOT_FOUND",
                                      "message": "No existe una ruta con el ID proporcionado",
                                      "timestamp": "2026-10-05T10:30:00Z"
                                    }
                                    """)))
    })
    @GetMapping("/{routeId}")
    public ResponseEntity<RouteDetailResponse> findById(
            @Parameter(description = "UUID de la ruta",
                    example = "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201")
            @PathVariable UUID routeId
    ) {
        log.debug("Find route — id={}", routeId);
        var response = routeService.findById(routeId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Planificar una ruta",
            description = "Asigna un vehiculo y un conductor a una ruta nueva. "
                    + "Valida que el vehiculo no este fuera de servicio, que el conductor este activo "
                    + "con licencia vigente y categoria compatible, y que no exista solapamiento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ruta planificada",
                    content = @Content(schema = @Schema(implementation = RouteDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404",
                    description = "Vehiculo o conductor inexistente (VEHICLE_NOT_FOUND o DRIVER_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "Conflicto de negocio (VEHICLE_NOT_AVAILABLE, DRIVER_NOT_ELIGIBLE, ROUTE_OVERLAP)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503",
                    description = "Dependencia caida (SERVICE_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RouteDetailResponse> create(
            @Valid @RequestBody CreateRouteRequest request
    ) {
        log.info("Plan route — vehicleId={}, driverId={}", request.vehicleId(), request.driverId());
        var response = routeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}