package es.bytescolab.msdashboard.controller;

import es.bytescolab.msdashboard.dto.response.DashboardResponse;
import es.bytescolab.msdashboard.dto.response.ErrorResponse;
import es.bytescolab.msdashboard.service.DashboardAggregator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "Resumen ejecutivo de la flota y orquestación de consultas")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardAggregator dashboardAggregator;

    @Operation(
            summary = "Resumen ejecutivo de la flota",
            description = "Agrega el inventario de la flota, KPIs operativos (rutas, combustible, mantenimiento), "
                    + "snapshot de alertas abiertas y próximas órdenes de mantenimiento en una sola respuesta. "
                    + "Si un servicio origen no responde, su bloque se reemplaza por null y se publica en "
                    + "`unavailable`, marcándose `partial=true`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen generado",
                    content = @Content(schema = @Schema(implementation = DashboardResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<DashboardResponse> getSummary(
            @Parameter(description = "Días del periodo a cubrir (1..365)",
                    example = "30")
            @RequestParam(value = "days", defaultValue = "30") @Min(1) @Max(365) int days
    ) {
        log.info("Petición GET /api/dashboard — days={}", days);
        DashboardResponse respuesta = dashboardAggregator.getSummary(days);
        return ResponseEntity.ok(respuesta);
    }
}
