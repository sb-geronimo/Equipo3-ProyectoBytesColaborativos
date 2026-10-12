package es.bytescolab.msalerts.service.impl;

import es.bytescolab.msalerts.dto.response.AlertResponse;
import es.bytescolab.msalerts.dto.response.PageResponse;
import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.exception.AlertNotFoundException;
import es.bytescolab.msalerts.exception.InvalidAlertStateException;
import es.bytescolab.msalerts.mapper.AlertMapper;
import es.bytescolab.msalerts.repository.AlertRepository;
import es.bytescolab.msalerts.repository.AlertSpecifications;
import es.bytescolab.msalerts.service.AlertQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertQueryServiceImpl implements AlertQueryService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> findAll(
            AlertStatus status,
            AlertSeverity severity,
            AlertType type,
            UUID vehicleId,
            int page,
            int size
    ) {
        Specification<AlertEntity> specification = AlertSpecifications.withFilters(status, severity, type, vehicleId);
        Sort sort = Sort.by(
                Sort.Order.asc("severityRank"),
                Sort.Order.desc("createdAt")
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        return PageResponse.from(alertRepository.searchAlerts(specification, pageable), alertMapper::toResponse);
    }

    @Override
    @Transactional
    public AlertResponse acknowledge(UUID alertId, String comment, UUID currentUserId) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new InvalidAlertStateException(alertId, alert.getStatus());
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        alert.setAcknowledgedBy(currentUserId);
        alert.setComment(comment);
        AlertEntity saved = alertRepository.save(alert);
        log.info("Alerta {} reconocida por {}", alertId, currentUserId);
        return alertMapper.toResponse(saved);
    }
}