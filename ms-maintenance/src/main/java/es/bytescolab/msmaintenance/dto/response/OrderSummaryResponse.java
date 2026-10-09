package es.bytescolab.msmaintenance.dto.response;

import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        UUID planId,
        UUID vehicleId,
        MaintenanceType type,
        MaintenanceOrderStatus status,
        LocalDate scheduledFor,
        BigDecimal cost
) {
}
