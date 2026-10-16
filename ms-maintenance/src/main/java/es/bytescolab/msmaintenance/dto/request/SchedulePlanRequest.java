package es.bytescolab.msmaintenance.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "SchedulePlanRequest", description = "Datos para programar un plan de mantenimiento")
public record SchedulePlanRequest(

        @Schema(description = "Identificador del vehículo", example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61")
        @NotNull(message = "El vehículo es obligatorio")
        UUID vehicleId,

        @Schema(description = "Tipo de mantenimiento", example = "OIL_CHANGE")
        @NotNull(message = "El tipo de mantenimiento es obligatorio")
        MaintenanceType type,

        @Schema(description = "Intervalo en kilómetros entre mantenimientos (opcional si se indica intervalDays)",
                example = "15000")
        @Positive(message = "El intervalo en kilómetros debe ser mayor que 0")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer intervalKm,

        @Schema(description = "Intervalo en días entre mantenimientos (opcional si se indica intervalKm)",
                example = "365")
        @Positive(message = "El intervalo en días debe ser mayor que 0")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer intervalDays,

        @Schema(description = "Fecha del último mantenimiento realizado", example = "2026-01-15")
        LocalDate lastDoneAt,

        @Schema(description = "Odómetro en km del último mantenimiento realizado", example = "30000")
        Integer lastDoneKm
) {

    @AssertTrue(message = "Debe indicar al menos un intervalo: kilómetros o días")
    public boolean hasInterval() {
        return intervalKm != null || intervalDays != null;
    }
}
