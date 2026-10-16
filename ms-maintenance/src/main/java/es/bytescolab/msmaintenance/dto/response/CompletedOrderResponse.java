package es.bytescolab.msmaintenance.dto.response;

import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "CompletedOrderResponse", description = "Orden de mantenimiento completada")
public record CompletedOrderResponse(
        @Schema(description = "Identificador de la orden", example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID id,

        @Schema(description = "Identificador del vehículo", example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61")
        UUID vehicleId,

        @Schema(description = "Tipo de mantenimiento", example = "OIL_CHANGE")
        MaintenanceType type,

        @Schema(description = "Estado de la orden", example = "COMPLETED")
        MaintenanceOrderStatus status,

        @Schema(description = "Marca de tiempo de finalización en UTC", example = "2026-10-16T11:40:00Z")
        Instant completedAt,

        @Schema(description = "Coste registrado en EUR", example = "249.99")
        BigDecimal cost,

        @Schema(description = "Lectura del odómetro en km al completar", example = "46230")
        Integer odometerKm,

        @Schema(description = "Taller donde se realizó el mantenimiento", example = "Talleres Central")
        String workshop,

        @Schema(description = "Plan con las fechas de vencimiento recalculadas")
        RecalculatedDueDatesResponse plan
) {
}
