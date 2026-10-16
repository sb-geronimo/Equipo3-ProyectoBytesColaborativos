package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(name = "TimeseriesResponse", description = "Series temporales múltiples solicitadas en una sola petición")
public record TimeseriesResponse(

        @Schema(description = "Fecha inicial del rango en UTC", example = "2026-09-07")
        LocalDate from,

        @Schema(description = "Fecha final del rango en UTC", example = "2026-10-14")
        LocalDate to,

        @Schema(description = "Granularidad aplicada a las series", example = "WEEK",
                allowableValues = {"DAY", "WEEK", "MONTH"})
        String granularity,

        @Schema(description = "Series temporales solicitadas (omitidas las métricas cuyo servicio no respondió)")
        List<TimeseriesSeries> series,

        @Schema(description = "Servicios origen que no respondieron", example = "[\"ms-fuel\"]")
        List<String> unavailable
) {
}
