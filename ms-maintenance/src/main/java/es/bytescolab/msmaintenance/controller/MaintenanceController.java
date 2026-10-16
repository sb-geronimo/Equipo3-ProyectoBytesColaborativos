package es.bytescolab.msmaintenance.controller;

import es.bytescolab.msmaintenance.dto.request.CompleteOrderRequest;
import es.bytescolab.msmaintenance.dto.request.MaintenanceStatsRequest;
import es.bytescolab.msmaintenance.dto.request.SchedulePlanRequest;
import es.bytescolab.msmaintenance.dto.response.*;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import es.bytescolab.msmaintenance.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Maintenance", description = "Planes de mantenimiento programado y órdenes de trabajo")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @Operation(
            summary = "Crear un plan de mantenimiento",
            description = "Registra un plan para un vehículo con al menos un intervalo (kilómetros o días) "
                    + "y calcula su próximo vencimiento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plan creado",
                    content = @Content(schema = @Schema(implementation = PlanDetailsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo inexistente (VEHICLE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un plan para ese vehículo y tipo "
                    + "(PLAN_ALREADY_EXISTS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "ms-vehicles no disponible (SERVICE_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/plans")
    public ResponseEntity<PlanDetailsResponse> plans(
            @Valid @RequestBody SchedulePlanRequest request
    ) {
        PlanDetailsResponse response = maintenanceService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Listar planes de mantenimiento",
            description = "Devuelve los planes filtrados por vehículo, tipo y/o estado activo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de planes"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/plans")
    public ResponseEntity<List<PlanDetailsResponse>> getPlans(
            @Parameter(description = "Filtro por vehículo")
            @RequestParam(required = false) UUID vehicle,

            @Parameter(description = "Filtro por tipo de mantenimiento", example = "OIL_CHANGE")
            @RequestParam(required = false) MaintenanceType type,

            @Parameter(description = "Filtro por planes activos o inactivos", example = "true")
            @RequestParam(required = false) Boolean active
    ) {
        List<PlanDetailsResponse> response = maintenanceService.listPlans(vehicle, type, active);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Listar órdenes de mantenimiento",
            description = "Página de órdenes ordenadas por scheduledFor ascendente, filtrable por vehículo, "
                    + "estado, tipo y fecha límite."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de órdenes",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/orders")
    public ResponseEntity<PageResponse<OrderSummaryResponse>> listOrders(
            @Parameter(description = "Filtro por vehículo")
            @RequestParam(required = false) UUID vehicle,

            @Parameter(description = "Filtro por estado de la orden", example = "PENDING")
            @RequestParam(required = false) MaintenanceOrderStatus status,

            @Parameter(description = "Filtro por tipo de mantenimiento", example = "OIL_CHANGE")
            @RequestParam(required = false) MaintenanceType type,

            @Parameter(description = "Solo órdenes con vencimiento en o antes de esta fecha", example = "2026-10-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate dueBefore,

            @Parameter(description = "Número de página (0 en adelante)", example = "0")
            @PositiveOrZero @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "Tamaño de página (por defecto 20, máximo 100)", example = "20")
            @Max(100) @RequestParam(defaultValue = "20") Integer size
    ) {
        PageResponse<OrderSummaryResponse> response =
                maintenanceService.findOrders(vehicle, status, type, dueBefore, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Iniciar una orden de mantenimiento",
            description = "Transiciona la orden de PENDING a IN_PROGRESS y marca el vehículo como IN_MAINTENANCE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden iniciada",
                    content = @Content(schema = @Schema(implementation = StartedOrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada (ORDER_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Estado de la orden no permitido (INVALID_ORDER_STATE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "ms-vehicles no disponible (SERVICE_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/orders/{orderId}/start")
    public ResponseEntity<StartedOrderResponse> startOrder(
            @Parameter(description = "UUID de la orden")
            @PathVariable UUID orderId) {
        StartedOrderResponse response = maintenanceService.startOrder(orderId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Completar una orden de mantenimiento",
            description = "Transiciona la orden de IN_PROGRESS a COMPLETED, devuelve el vehículo a AVAILABLE "
                    + "con el odómetro indicado y recalcula el próximo vencimiento del plan."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden completada",
                    content = @Content(schema = @Schema(implementation = CompletedOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada (ORDER_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Estado no permitido u odómetro inválido "
                    + "(INVALID_ORDER_STATE / INVALID_ODOMETER)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "ms-vehicles no disponible (SERVICE_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/orders/{orderId}/complete")
    public ResponseEntity<CompletedOrderResponse> completeOrder(
            @Parameter(description = "UUID de la orden")
            @PathVariable UUID orderId,
            @Valid @RequestBody CompleteOrderRequest request
    ) {
        CompletedOrderResponse response = maintenanceService.completeOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Serie temporal de órdenes y costes",
            description = "Agrega órdenes y costes por periodo (DAY, WEEK o MONTH) dentro del rango solicitado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serie temporal generada",
                    content = @Content(schema = @Schema(implementation = MaintenanceStatsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<MaintenanceStatsResponse> getStats(
            @Parameter(description = "Fecha inicial del rango (inclusiva)", example = "2026-09-07")
            @RequestParam("from") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Fecha final del rango (inclusiva)", example = "2026-09-27")
            @RequestParam("to") @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Granularidad de agrupación: DAY, WEEK o MONTH", example = "WEEK")
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY")
            String granularity,

            @Parameter(description = "Filtra la serie por vehículo")
            @RequestParam(value = "vehicle", required = false) UUID vehicle
    ) {
        MaintenanceStatsRequest request =
                new MaintenanceStatsRequest(from, to, granularity, vehicle);
        MaintenanceStatsResponse response = maintenanceService.getStats(request);
        return ResponseEntity.ok(response);
    }
}
