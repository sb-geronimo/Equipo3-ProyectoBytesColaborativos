package es.bytescolab.msfuel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(name = "FuelStatsResponse", description = "Serie temporal de repostajes")
public record FuelStatsResponse(

        @Schema(description = "Fecha inicial del rango en UTC", example = "2026-09-01")
        LocalDate from,

        @Schema(description = "Fecha final del rango en UTC", example = "2026-09-30")
        LocalDate to,

        @Schema(description = "Granularidad aplicada a la serie", example = "WEEK",
                allowableValues = {"DAY", "WEEK", "MONTH"})
        String granularity,

        @Schema(description = "Totales agregados para el rango solicitado")
        FuelStatsTotals totals,

        @Schema(description = "Puntos de la serie temporal ordenados cronológicamente")
        List<FuelStatsPoint> series
) {
}
