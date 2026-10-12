package es.bytescolab.msfuel.controller;

import es.bytescolab.msfuel.dto.request.RefuelConsumptionRequest;
import es.bytescolab.msfuel.dto.request.RefuelCreateRequest;
import es.bytescolab.msfuel.dto.request.RefuelFilterRequest;
import es.bytescolab.msfuel.dto.request.RefuelStatsRequest;
import es.bytescolab.msfuel.dto.response.ConsumptionResponse;
import es.bytescolab.msfuel.dto.response.ErrorResponse;
import es.bytescolab.msfuel.dto.response.FuelStatsResponse;
import es.bytescolab.msfuel.dto.response.PageResponse;
import es.bytescolab.msfuel.dto.response.RefuelResponse;
import es.bytescolab.msfuel.service.FuelRecordService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Fuel", description = "Repostajes y consumo de combustible")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fuel")
public class FuelController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_PAGE_SIZE = "20";

    private final FuelRecordService fuelRecordService;

    @Operation(
            summary = "Registrar un repostaje",
            description = "Valida el vehículo en ms-vehicles, calcula totalCost y consumptionL100km "
                    + "respecto al último repostaje con depósito lleno del mismo vehículo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Repostaje registrado",
                    content = @Content(schema = @Schema(implementation = RefuelResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo inexistente (VEHICLE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "Odómetro menor que el del repostaje anterior (INVALID_ODOMETER)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503",
                    description = "ms-vehicles no disponible (SERVICE_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refuels")
    public ResponseEntity<RefuelResponse> createRefuel(
            @Valid @RequestBody RefuelCreateRequest request
    ) {
        log.info("Petición POST /api/fuel/refuels recibida — vehicleId={}, refueledAt={}, liters={}",
                request.vehicleId(), request.refueledAt(), request.liters());
        RefuelResponse response = fuelRecordService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Listar repostajes paginados",
            description = "Listado de repostajes ordenado por refueledAt descendente. Permite filtrar "
                    + "por vehículo y por rango de fechas sobre refueledAt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de repostajes",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Parámetros de filtrado o paginación inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/refuels")
    public ResponseEntity<PageResponse<RefuelResponse>> findRefuels(
            @Parameter(description = "Filtra por identificador de vehículo",
                    example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
            @RequestParam(value = "vehicle", required = false) UUID vehicle,

            @Parameter(description = "Fecha inicial (inclusiva) sobre refueledAt",
                    example = "2026-09-01")
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Fecha final (inclusiva) sobre refueledAt",
                    example = "2026-09-30")
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Número de página (0 en adelante)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página (por defecto 20, máximo 100)", example = "20")
            @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) int size
    ) {
        RefuelFilterRequest filter = new RefuelFilterRequest(vehicle, from, to, page, size);
        log.info("Petición GET /api/fuel/refuels recibida — vehicle={}, from={}, to={}, page={}, size={}",
                filter.vehicle(), filter.from(), filter.to(), filter.page(), filter.size());

        int safePage = filter.page();
        int safeSize = Math.min(Math.max(filter.size(), 1), MAX_PAGE_SIZE);
        Page<RefuelResponse> pageResult = fuelRecordService.findAll(
                filter.vehicle(), filter.from(), filter.to(), safePage, safeSize);
        return ResponseEntity.ok(PageResponse.of(pageResult));
    }

    @Operation(
            summary = "Consumo agregado por vehículo",
            description = "Distancia calculada como la diferencia de odómetro entre el primer y el último "
                    + "repostaje del periodo. La placa y el tipo se completan con datos de ms-vehicles. "
                    + "Lo consumen ms-dashboard y ms-alerts."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consumo agregado por vehículo",
                    content = @Content(schema = @Schema(implementation = ConsumptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/consumption")
    public ResponseEntity<ConsumptionResponse> getConsumption(
            @Parameter(description = "Fecha inicial del rango (inclusiva)", example = "2026-09-01")
            @RequestParam("from") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Fecha final del rango (inclusiva)", example = "2026-09-30")
            @RequestParam("to") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        log.info("Petición GET /api/fuel/consumption recibida — from={}, to={}", from, to);
        RefuelConsumptionRequest request = new RefuelConsumptionRequest(from, to);
        ConsumptionResponse response = fuelRecordService.getConsumption(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Serie temporal de repostajes",
            description = "Litros, coste y consumo medio de la flota o de un vehículo con granularidad "
                    + "DAY, WEEK o MONTH. Lo consume ms-dashboard."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serie temporal generada",
                    content = @Content(schema = @Schema(implementation = FuelStatsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<FuelStatsResponse> getStats(
            @Parameter(description = "Fecha inicial del rango (inclusiva)", example = "2026-09-01")
            @RequestParam("from") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Fecha final del rango (inclusiva)", example = "2026-09-30")
            @RequestParam("to") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Granularidad de la serie temporal",
                    example = "DAY", schema = @Schema(allowableValues = {"DAY", "WEEK", "MONTH"}))
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY")
            String granularity,

            @Parameter(description = "Filtra la serie por identificador de vehículo",
                    example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
            @RequestParam(value = "vehicle", required = false) UUID vehicle
    ) {
        log.info("Petición GET /api/fuel/stats recibida — from={}, to={}, granularity={}, vehicle={}",
                from, to, granularity, vehicle);

        RefuelStatsRequest request = new RefuelStatsRequest(from, to, granularity, vehicle);
        FuelStatsResponse response = fuelRecordService.getStats(request);
        return ResponseEntity.ok(response);
    }
}
