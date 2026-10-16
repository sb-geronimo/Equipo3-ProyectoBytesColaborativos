package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "DashboardPeriodBlock", description = "Periodo cubierto por el resumen del dashboard")
public record DashboardPeriodBlock(

        @Schema(description = "Fecha inicial del rango (inclusiva) en UTC",
                example = "2026-09-05")
        LocalDate from,

        @Schema(description = "Fecha final del rango (inclusiva) en UTC",
                example = "2026-10-05")
        LocalDate to
) {
}
