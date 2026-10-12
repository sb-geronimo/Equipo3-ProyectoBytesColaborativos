package es.bytescolab.msalerts.service.impl;

import es.bytescolab.msalerts.dto.response.EvaluationSummaryResponse;
import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.repository.AlertRuleRepository;
import es.bytescolab.msalerts.service.AlertEvaluationService;
import es.bytescolab.msalerts.service.helper.AlertEvaluatorRegistry;
import es.bytescolab.msalerts.service.helper.AlertPersistenceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEvaluationServiceImpl implements AlertEvaluationService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertEvaluatorRegistry evaluatorRegistry;
    private final AlertPersistenceHelper persistenceHelper;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EvaluationSummaryResponse runEvaluation() {
        Instant startedAt = Instant.now(clock);
        log.info("Iniciando evaluación de alertas");

        List<AlertRuleEntity> rules = alertRuleRepository.findAll();
        List<String> skipped = new ArrayList<>();
        List<es.bytescolab.msalerts.service.helper.AlertCandidate> allCandidates = new ArrayList<>();

        for (AlertRuleEntity rule : rules) {
            if (Boolean.FALSE.equals(rule.getEnabled())) {
                continue;
            }
            AlertEvaluatorRegistry.EvaluationOutcome outcome = evaluatorRegistry.run(rule);
            allCandidates.addAll(outcome.candidates());
            if (outcome.skipped()) {
                skipped.add(rule.getType().name());
            }
        }

        AlertPersistenceHelper.PersistenceResult persistence =
                persistenceHelper.apply(allCandidates);

        long durationMs = Duration.between(startedAt, Instant.now(clock)).toMillis();
        log.info("Evaluación completada: created={}, resolved={}, skipped={}, durationMs={}",
                persistence.created(), persistence.resolved(), skipped, durationMs);

        return new EvaluationSummaryResponse(
                persistence.created(),
                persistence.resolved(),
                skipped,
                durationMs
        );
    }
}