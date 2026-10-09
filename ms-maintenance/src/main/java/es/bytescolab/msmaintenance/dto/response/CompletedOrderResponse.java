package es.bytescolab.msmaintenance.dto.response;

import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CompletedOrderResponse(
        UUID id,
        UUID vehicleId,
        MaintenanceType type,
        MaintenanceOrderStatus status,
        Instant completedAt,
        BigDecimal cost,
        Integer odometerKm,
        String workshop,
        RecalculatedDueDatesResponse plan
) {
}
