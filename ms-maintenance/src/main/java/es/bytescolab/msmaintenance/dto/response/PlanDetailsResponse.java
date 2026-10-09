package es.bytescolab.msmaintenance.dto.response;

import es.bytescolab.msmaintenance.enums.MaintenanceType;

import java.time.LocalDate;
import java.util.UUID;

public record PlanDetailsResponse(
        UUID id,
        UUID vehicleId,
        MaintenanceType type,
        Integer intervalKm,
        Integer intervalDays,
        LocalDate lastDoneAt,
        Integer lastDoneKm,
        LocalDate nextDueAt,
        Integer nextDueKm,
        Boolean active
) {
}
