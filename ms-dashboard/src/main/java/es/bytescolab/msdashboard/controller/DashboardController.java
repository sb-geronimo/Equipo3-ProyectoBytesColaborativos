package es.bytescolab.msdashboard.controller;

import es.bytescolab.msdashboard.dto.request.MetricsTimeseriesRequest;
import es.bytescolab.msdashboard.dto.response.DashboardResponse;
import es.bytescolab.msdashboard.dto.response.ErrorResponse;
import es.bytescolab.msdashboard.dto.response.RankingResponse;
import es.bytescolab.msdashboard.dto.response.TimeseriesResponse;
import es.bytescolab.msdashboard.enums.DashboardMetric;
import es.bytescolab.msdashboard.enums.RankingMetric;
import es.bytescolab.msdashboard.service.DashboardAggregator;
import es.bytescolab.msdashboard.service.RankingService;
import es.bytescolab.msdashboard.service.TimeseriesService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Dashboard", description = "Resumen ejecutivo de la flota y orquestación de consultas")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardAggregator dashboardAggregator;
    private final TimeseriesService timeseriesService;
    private final RankingService rankingService;

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

    @Operation(
            summary = "Series temporales por métrica",
            description = "Devuelve una serie temporal por métrica solicitada. Solo se llaman a los servicios "
                    + "origen de las métricas presentes en la query. Si un servicio origen no responde, "
                    + "sus series se omiten y se reporta en `unavailable`. Rango máximo: 366 días."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Series temporales generadas",
                    content = @Content(schema = @Schema(implementation = TimeseriesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/timeseries")
    public ResponseEntity<TimeseriesResponse> getTimeseries(
            @Parameter(description = "Lista de métricas separadas por coma (DISTANCE_KM, FUEL_LITERS, "
                    + "FUEL_COST, CONSUMPTION_L100KM, MAINTENANCE_COST, ALERTS_CREATED)",
                    example = "DISTANCE_KM,FUEL_COST")
            @RequestParam("metrics") List<DashboardMetric> metrics,

            @Parameter(description = "Fecha inicial del rango (inclusiva)", example = "2026-09-01")
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Fecha final del rango (inclusiva)", example = "2026-10-14")
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Granularidad de la serie temporal",
                    example = "DAY", schema = @Schema(allowableValues = {"DAY", "WEEK", "MONTH"}))
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY")
            String granularity
    ) {
        log.info("Petición GET /api/dashboard/timeseries — metrics={}, from={}, to={}, granularity={}",
                metrics, from, to, granularity);
        MetricsTimeseriesRequest request = new MetricsTimeseriesRequest(metrics, from, to, granularity);
        TimeseriesResponse respuesta = timeseriesService.getTimeseries(request);
        return ResponseEntity.ok(respuesta);
    }

    @Operation(
            summary = "Ranking de vehículos por coste de combustible o consumo",
            description = "Llama a ms-fuel /api/fuel/consumption, ordena los vehículos de mayor a menor "
                    + "según la métrica (FUEL_COST o CONSUMPTION) y aplica un filtro opcional por tipo. "
                    + "Devuelve los primeros `limit` (1..20)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking generado",
                    content = @Content(schema = @Schema(implementation = RankingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/vehicles/ranking")
    public ResponseEntity<RankingResponse> getRanking(
            @Parameter(description = "Métrica del ranking", example = "FUEL_COST",
                    schema = @Schema(allowableValues = {"FUEL_COST", "CONSUMPTION"}))
            @RequestParam("metric") RankingMetric metric,

            @Parameter(description = "Días del periodo a cubrir (1..365)", example = "30")
            @RequestParam(value = "days", defaultValue = "30") @Min(1) @Max(365) int days,

            @Parameter(description = "Número máximo de entradas (1..20)", example = "5")
            @RequestParam(value = "limit", defaultValue = "5") @Min(1) @Max(20) int limit,

            @Parameter(description = "Filtra el ranking por tipo de vehículo (opcional)",
                    example = "VAN")
            @RequestParam(value = "type", required = false) String type
    ) {
        log.info("Petición GET /api/dashboard/vehicles/ranking — metric={}, days={}, limit={}, type={}",
                metric, days, limit, type);
        RankingResponse respuesta = rankingService.getRanking(metric, days, limit, type);
        return ResponseEntity.ok(respuesta);
    }
}
