package es.bytescolab.msmaintenance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "MaintenanceStatsUpcoming", description = "Snapshot de órdenes pendientes: vencidas y próximas a vencer")
public record MaintenanceStatsUpcoming(

        @Schema(description = "Órdenes pendientes con fecha programada anterior a hoy (UTC)",
                example = "1")
        long overdue,

        @Schema(description = "Órdenes pendientes programadas entre hoy y hoy+7 días (inclusivo, UTC)",
                example = "5")
        long dueIn7Days
) {
}