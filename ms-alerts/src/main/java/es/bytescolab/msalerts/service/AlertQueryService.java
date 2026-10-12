package es.bytescolab.msalerts.service;

import es.bytescolab.msalerts.dto.response.AlertResponse;
import es.bytescolab.msalerts.dto.response.PageResponse;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;

import java.util.UUID;

public interface AlertQueryService {

    PageResponse<AlertResponse> findAll(
            AlertStatus status,
            AlertSeverity severity,
            AlertType type,
            UUID vehicleId,
            int page,
            int size
    );

    AlertResponse acknowledge(UUID alertId, String comment, UUID currentUserId);
}