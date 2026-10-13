package es.bytescolab.msmaintenance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Schema(name = "MaintenanceStatsPoint", description = "Punto de la serie temporal de órdenes completadas")
public record MaintenanceStatsPoint(

        @Schema(description = "Inicio del periodo (en UTC) al que se refiere el punto",
                example = "2026-09-07")
        LocalDate period,

        @Schema(description = "Número de órdenes completadas dentro del periodo", example = "3")
        long orders,

        @Schema(description = "Coste total acumulado dentro del periodo, en euros",
                example = "420.00")
        BigDecimal cost
) {
}