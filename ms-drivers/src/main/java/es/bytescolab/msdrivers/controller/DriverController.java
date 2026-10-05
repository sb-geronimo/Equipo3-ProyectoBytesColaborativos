package es.bytescolab.msdrivers.controller;

import es.bytescolab.msdrivers.dto.request.DriverCreateRequest;
import es.bytescolab.msdrivers.dto.request.DriverUpdateRequest;
import es.bytescolab.msdrivers.dto.response.DriverDetailResponse;
import es.bytescolab.msdrivers.dto.response.DriverSummaryResponse;
import es.bytescolab.msdrivers.dto.response.ErrorResponse;
import es.bytescolab.msdrivers.dto.response.PageResponse;
import es.bytescolab.msdrivers.enums.DriverStatus;
import es.bytescolab.msdrivers.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Conductores",
        description = "Gestión de conductores, licencias y fechas de caducidad")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drivers")
public class DriverController {

    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final int MAX_PAGE_SIZE = 100;

    private final DriverService driverService;

    @Operation(
            summary = "Listar conductores",
            description = "Lista paginada de conductores, filtrable por estado o por caducidad de la licencia. "
                    + "Cuando se indica `licenseExpiringInDays` se devuelven los conductores cuya licencia "
                    + "caduca en ese número de días o menos, incluidas las ya caducadas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de conductores",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<DriverSummaryResponse>> findAll(
            @Parameter(description = "Filtro por estado del conductor", example = "ACTIVE")
            @RequestParam(value = "status", required = false) DriverStatus status,

            @Parameter(description = "Devuelve los conductores cuya licencia caduca en N días o menos, "
                    + "incluidas las ya caducadas", example = "30")
            @RequestParam(value = "licenseExpiringInDays", required = false)
            @Min(value = 0, message = "licenseExpiringInDays debe ser mayor o igual que 0")
            Integer licenseExpiringInDays,

            @Parameter(description = "Número de página (0 en adelante)", example = "0")
            @RequestParam(value = "page", defaultValue = "0")
            @Min(value = 0, message = "page debe ser mayor o igual que 0")
            int page,

            @Parameter(description = "Tamaño de página (por defecto 20, máximo 100)", example = "20")
            @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE)
            @Min(value = 1, message = "size debe ser mayor que 0")
            @Max(value = MAX_PAGE_SIZE,
                    message = "size no puede ser superior a " + MAX_PAGE_SIZE)
            int size
    ) {
        log.debug("List drivers — status={}, licenseExpiringInDays={}, page={}, size={}",
                status, licenseExpiringInDays, page, size);

        Pageable pageable = Pageable.ofSize(size).withPage(page);

        var response = driverService.findAll(status, licenseExpiringInDays, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Detalle de un conductor",
            description = "Devuelve el detalle completo del conductor, incluidos `createdAt` y `updatedAt`. "
                    + "Lo consumen ms-routes (validación de asignación) y ms-alerts (caducidad de licencia)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conductor encontrado",
                    content = @Content(schema = @Schema(implementation = DriverDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conductor no encontrado (DRIVER_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "No existe", value = """
                                    {
                                      "error": "DRIVER_NOT_FOUND",
                                      "message": "No existe un conductor con el ID proporcionado",
                                      "timestamp": "2026-10-05T10:30:00Z"
                                    }
                                    """)))
    })
    @GetMapping("/{driverId}")
    public ResponseEntity<DriverDetailResponse> findById(
            @Parameter(description = "UUID del conductor",
                    example = "3b2d8f10-7c4a-4e55-bla0-5d9e2c7f1a01")
            @PathVariable UUID driverId
    ) {
        log.debug("Find driver — id={}", driverId);
        var response = driverService.findById(driverId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Dar de alta un conductor",
            description = "Crea un conductor nuevo. El estado inicial siempre es ACTIVE. "
                    + "El número de licencia debe ser único y la fecha de caducidad posterior a la fecha actual."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conductor creado",
                    content = @Content(schema = @Schema(implementation = DriverDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Licencia caducada", value = """
                                    {
                                      "error": "VALIDATION_ERROR",
                                      "message": "La petición contiene campos no válidos",
                                      "timestamp": "2026-10-05T10:30:00Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "409", description = "Número de licencia duplicado (DRIVER_ALREADY_EXISTS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Licencia duplicada", value = """
                                    {
                                      "error": "DRIVER_ALREADY_EXISTS",
                                      "message": "Ya existe un conductor con ese número de licencia",
                                      "timestamp": "2026-10-05T10:30:00Z"
                                    }
                                    """)))
    })
    @PostMapping
    public ResponseEntity<DriverDetailResponse> create(
            @Valid @RequestBody DriverCreateRequest request
    ) {
        log.info("Create driver — licenseNumber={}", request.licenseNumber());
        var response = driverService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Actualizar un conductor",
            description = "Actualiza todos los datos del conductor, incluido el estado "
                    + "(ACTIVE, ON_LEAVE, SUSPENDED). Permite renovar la licencia con "
                    + "una nueva fecha de caducidad, que debe ser posterior a hoy."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conductor actualizado",
                    content = @Content(schema = @Schema(implementation = DriverDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conductor no encontrado (DRIVER_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Número de licencia duplicado (DRIVER_ALREADY_EXISTS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{driverId}")
    public ResponseEntity<DriverDetailResponse> update(
            @Parameter(description = "UUID del conductor",
                    example = "3b2d8f10-7c4a-4e55-bla0-5d9e2c7f1a01")
            @PathVariable UUID driverId,

            @Valid @RequestBody DriverUpdateRequest request
    ) {
        log.info("Update driver — id={}", driverId);
        DriverDetailResponse response = driverService.update(driverId, request);
        return ResponseEntity.ok(response);
    }
}