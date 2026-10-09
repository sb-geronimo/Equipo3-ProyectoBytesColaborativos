package es.bytescolab.msmaintenance.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record SchedulePlanRequest(
        @NotNull(message = "El vehículo es obligatorio")
        UUID vehicleId,
        @NotNull(message = "El tipo de mantenimiento es obligatorio")
        MaintenanceType type,
        @Positive(message = "El intervalo en kilómetros debe ser mayor que 0")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer intervalKm,
        @Positive(message = "El intervalo en días debe ser mayor que 0")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer intervalDays,
        LocalDate lastDoneAt,
        Integer lastDoneKm
) {

    @AssertTrue(message = "Debe indicar al menos un intervalo: kilómetros o días")
    public boolean hasInterval() {
        return intervalKm != null || intervalDays != null;
    }
}
