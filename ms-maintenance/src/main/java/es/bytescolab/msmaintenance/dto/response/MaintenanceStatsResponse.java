package es.bytescolab.msmaintenance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(name = "MaintenanceStatsResponse", description = "Serie temporal de órdenes de mantenimiento completadas")
public record MaintenanceStatsResponse(

        @Schema(description = "Fecha inicial del rango en UTC", example = "2026-09-07")
        LocalDate from,

        @Schema(description = "Fecha final del rango en UTC", example = "2026-09-27")
        LocalDate to,

        @Schema(description = "Granularidad aplicada a la serie", example = "WEEK",
                allowableValues = {"DAY", "WEEK", "MONTH"})
        String granularity,

        @Schema(description = "Totales agregados para el rango solicitado")
        MaintenanceStatsTotals totals,

        @Schema(description = "Puntos de la serie temporal ordenados cronológicamente")
        List<MaintenanceStatsPoint> series,

        @Schema(description = "Desglose por tipo de mantenimiento dentro del rango")
        List<MaintenanceStatsByType> byType,

        @Schema(description = "Snapshot independiente de órdenes pendientes: vencidas y próximas a vencer")
        MaintenanceStatsUpcoming upcoming
) {
}