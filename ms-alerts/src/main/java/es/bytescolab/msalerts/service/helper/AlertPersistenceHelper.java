package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertStatus;
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

    public PersistenceResult apply(List<AlertCandidate> candidates) {
        if (candidates.isEmpty()) {
            return new PersistenceResult(0, 0, List.of());
        }

        Set<String> dedupKeys = candidates.stream()
                .map(AlertCandidate::dedupKey)
                .collect(Collectors.toSet());
        Map<String, AlertEntity> activeByDedup = alertRepository.findActiveByDedupKeys(dedupKeys).stream()
                .collect(Collectors.toMap(AlertEntity::getDedupKey, Function.identity()));

        List<AlertEntity> toCreate = new ArrayList<>();
        Set<String> candidateDedupSet = new HashSet<>();
        for (AlertCandidate candidate : candidates) {
            if (!candidateDedupSet.add(candidate.dedupKey())) {
                continue;
            }
            if (!activeByDedup.containsKey(candidate.dedupKey())) {
                toCreate.add(buildEntity(candidate));
            }
        }

        List<AlertEntity> toResolve = new ArrayList<>();
        for (Map.Entry<String, AlertEntity> entry : activeByDedup.entrySet()) {
            if (!candidateDedupSet.contains(entry.getKey())) {
                AlertEntity alert = entry.getValue();
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(Instant.now(clock));
                toResolve.add(alert);
            }
        }

        if (!toCreate.isEmpty()) {
            alertRepository.saveAll(toCreate);
        }
        if (!toResolve.isEmpty()) {
            alertRepository.saveAll(toResolve);
        }
        log.debug("Persistencia aplicada: {} creadas, {} resueltas", toCreate.size(), toResolve.size());
        return new PersistenceResult(toCreate.size(), toResolve.size(), dedupKeys);
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

    public record PersistenceResult(int created, int resolved, Collection<String> processedDedupKeys) {
    }
}