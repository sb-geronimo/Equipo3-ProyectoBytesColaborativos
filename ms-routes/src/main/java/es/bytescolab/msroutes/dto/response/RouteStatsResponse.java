package es.bytescolab.msroutes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(name = "RouteStatsResponse", description = "Serie temporal de rutas completadas")
public record RouteStatsResponse(

        @Schema(description = "Fecha inicial del rango en UTC", example = "2026-10-01")
        LocalDate from,

        @Schema(description = "Fecha final del rango en UTC", example = "2026-10-31")
        LocalDate to,

        @Schema(description = "Granularidad aplicada a la serie", example = "DAY",
                allowableValues = {"DAY", "WEEK", "MONTH"})
        String granularity,

        @Schema(description = "Totales agregados para el rango solicitado")
        RouteStatsTotals totals,

        @Schema(description = "Puntos de la serie temporal ordenados cronológicamente")
        List<RouteStatsPoint> series
) {
}
