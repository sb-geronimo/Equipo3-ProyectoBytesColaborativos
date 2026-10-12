package es.bytescolab.msfuel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Schema(name = "RefuelStatsRequest", description = "Filtros para la serie temporal de repostajes")
public record RefuelStatsRequest(

        @NotNull
        @Schema(description = "Fecha inicial del rango (inclusiva)", example = "2026-09-01")
        LocalDate from,

        @NotNull
        @Schema(description = "Fecha final del rango (inclusiva)", example = "2026-09-30")
        LocalDate to,

        @Schema(description = "Granularidad de agrupación: DAY, WEEK o MONTH",
                example = "DAY", allowableValues = {"DAY", "WEEK", "MONTH"})
        String granularity,

        @Schema(description = "Identificador del vehículo para filtrar la serie",
                example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
        UUID vehicle
) {
    public RefuelStatsRequest {
        if (granularity == null || granularity.isBlank()) granularity = "DAY";
    }
}
