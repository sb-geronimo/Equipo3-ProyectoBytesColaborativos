package es.bytescolab.msmaintenance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Schema(name = "MaintenanceStatsRequest", description = "Filtros para la serie temporal de órdenes de mantenimiento")
public record MaintenanceStatsRequest(

        @NotNull
        @Schema(description = "Fecha inicial del rango (inclusiva)", example = "2026-09-07")
        LocalDate from,

        @NotNull
        @Schema(description = "Fecha final del rango (inclusiva)", example = "2026-09-27")
        LocalDate to,

        @Schema(description = "Granularidad de agrupación: DAY, WEEK o MONTH",
                example = "WEEK", allowableValues = {"DAY", "WEEK", "MONTH"})
        String granularity,

        @Schema(description = "Identificador del vehículo para filtrar la serie",
                example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61")
        UUID vehicle
) {
    public MaintenanceStatsRequest {
        if (granularity == null || granularity.isBlank()) granularity = "DAY";
    }
}