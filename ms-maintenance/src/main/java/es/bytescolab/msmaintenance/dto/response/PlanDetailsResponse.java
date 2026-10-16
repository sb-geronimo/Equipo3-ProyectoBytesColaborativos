package es.bytescolab.msmaintenance.dto.response;

import es.bytescolab.msmaintenance.enums.MaintenanceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "PlanDetailsResponse", description = "Detalle de un plan de mantenimiento")
public record PlanDetailsResponse(
        @Schema(description = "Identificador del plan", example = "9d2f7c10-4b8a-4e55-9c21-7a6d5e4f3b01")
        UUID id,

        @Schema(description = "Identificador del vehículo", example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61")
        UUID vehicleId,

        @Schema(description = "Tipo de mantenimiento", example = "OIL_CHANGE")
        MaintenanceType type,

        @Schema(description = "Intervalo en kilómetros", example = "15000")
        Integer intervalKm,

        @Schema(description = "Intervalo en días", example = "365")
        Integer intervalDays,

        @Schema(description = "Fecha del último mantenimiento", example = "2026-01-15")
        LocalDate lastDoneAt,

        @Schema(description = "Odómetro en km del último mantenimiento", example = "30000")
        Integer lastDoneKm,

        @Schema(description = "Próximo vencimiento por fecha", example = "2027-01-15")
        LocalDate nextDueAt,

        @Schema(description = "Próximo vencimiento por kilómetros", example = "45000")
        Integer nextDueKm,

        @Schema(description = "Indica si el plan está activo", example = "true")
        Boolean active
) {
}
