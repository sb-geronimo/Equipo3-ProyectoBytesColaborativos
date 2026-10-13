package es.bytescolab.msalerts.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(name = "AlertStatsRequest", description = "Filtros para la serie temporal de alertas creadas")
public record AlertStatsRequest(

        @Schema(description = "Fecha inicial del rango (inclusiva)", example = "2026-09-07")
        LocalDate from,

        @Schema(description = "Fecha final del rango (inclusiva)", example = "2026-09-27")
        LocalDate to,

        @Schema(description = "Granularidad de agrupación: DAY, WEEK o MONTH",
                example = "DAY", allowableValues = {"DAY", "WEEK", "MONTH"})
        String granularity
) {
    public AlertStatsRequest {
        if (granularity == null || granularity.isBlank()) {
            granularity = "DAY";
        } else {
            granularity = granularity.toUpperCase();
        }
    }
}
