package es.bytescolab.msalerts.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(name = "AlertStatsResponse", description = "Serie temporal de alertas creadas")
public record AlertStatsResponse(

        @Schema(description = "Fecha inicial del rango en UTC", example = "2026-09-07")
        LocalDate from,

        @Schema(description = "Fecha final del rango en UTC", example = "2026-09-27")
        LocalDate to,

        @Schema(description = "Granularidad aplicada a la serie", example = "WEEK",
                allowableValues = {"DAY", "WEEK", "MONTH"})
        String granularity,

        @Schema(description = "Totales agregados para el rango solicitado")
        AlertStatsTotals totals,

        @Schema(description = "Puntos de la serie temporal ordenados cronológicamente")
        List<AlertStatsPoint> series,

        @Schema(description = "Snapshot de alertas actualmente en estado OPEN agrupadas por severidad")
        AlertStatsOpen open
) {
}
