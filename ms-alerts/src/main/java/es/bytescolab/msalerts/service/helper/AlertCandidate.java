package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertType;

import java.util.UUID;

public record AlertCandidate(
        AlertType type,
        UUID vehicleId,
        UUID driverId,
        String dedupKey,
        String message,
        AlertSeverity severity
) {
}