package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertPersistenceHelper {

    private final AlertRepository alertRepository;
    private final Clock clock;

    public PersistenceResult apply(List<AlertCandidate> candidates, Collection<AlertType> evaluatedTypes) {
        Set<String> dedupKeys = candidates.stream()
                .map(AlertCandidate::dedupKey)
                .collect(Collectors.toSet());
        Map<String, AlertEntity> existingByDedup = alertRepository.findByDedupKeys(dedupKeys).stream()
                .collect(Collectors.toMap(AlertEntity::getDedupKey, Function.identity()));

        List<AlertEntity> toCreate = new ArrayList<>();
        List<AlertEntity> toReopen = new ArrayList<>();
        Set<String> candidateDedupSet = new HashSet<>();
        for (AlertCandidate candidate : candidates) {
            if (!candidateDedupSet.add(candidate.dedupKey())) {
                continue;
            }
            AlertEntity existing = existingByDedup.get(candidate.dedupKey());
            if (existing == null) {
                toCreate.add(buildEntity(candidate));
            } else if (!isActive(existing.getStatus())) {
                reopen(existing);
                toReopen.add(existing);
            }
        }

        List<AlertEntity> toResolve = new ArrayList<>();
        for (AlertEntity alert : alertRepository.findActive()) {
            if (evaluatedTypes.contains(alert.getType()) && !candidateDedupSet.contains(alert.getDedupKey())) {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(Instant.now(clock));
                toResolve.add(alert);
            }
        }

        if (!toCreate.isEmpty()) {
            alertRepository.saveAll(toCreate);
        }
        if (!toReopen.isEmpty()) {
            alertRepository.saveAll(toReopen);
        }
        if (!toResolve.isEmpty()) {
            alertRepository.saveAll(toResolve);
        }
        log.debug("Persistencia aplicada: {} creadas, {} reabiertas, {} resueltas",
                toCreate.size(), toReopen.size(), toResolve.size());
        return new PersistenceResult(toCreate.size(), toResolve.size(), toReopen.size(),
                dedupKeys, List.copyOf(toCreate), List.copyOf(toReopen));
    }

    private boolean isActive(AlertStatus status) {
        return status == AlertStatus.OPEN || status == AlertStatus.ACKNOWLEDGED;
    }

    private void reopen(AlertEntity alert) {
        alert.setStatus(AlertStatus.OPEN);
        alert.setResolvedAt(null);
        alert.setAcknowledgedAt(null);
        alert.setAcknowledgedBy(null);
        alert.setComment(null);
    }

    private AlertEntity buildEntity(AlertCandidate candidate) {
        return AlertEntity.builder()
                .id(UUID.randomUUID())
                .type(candidate.type())
                .severity(candidate.severity())
                .status(AlertStatus.OPEN)
                .vehicleId(candidate.vehicleId())
                .driverId(candidate.driverId())
                .message(candidate.message())
                .dedupKey(candidate.dedupKey())
                .createdAt(Instant.now(clock))
                .build();
    }

    public record PersistenceResult(int created, int resolved, int reopened,
                                      Collection<String> processedDedupKeys, List<AlertEntity> createdAlerts,
                                      List<AlertEntity> reopenedAlerts) {
    }
}