package es.bytescolab.msmaintenance.dto.response;

import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StartedOrderResponse(
        UUID id,
        UUID planId,
        UUID vehicleId,
        MaintenanceType type,
        MaintenanceOrderStatus status,
        LocalDate scheduledFor,
        Instant startedAt
) {
}
