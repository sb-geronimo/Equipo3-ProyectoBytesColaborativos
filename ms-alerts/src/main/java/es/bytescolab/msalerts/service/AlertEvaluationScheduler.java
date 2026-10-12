package es.bytescolab.msalerts.service;

import es.bytescolab.msalerts.dto.response.EvaluationSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEvaluationScheduler {

    private final AlertEvaluationService alertEvaluationService;

    @Scheduled(
            fixedDelayString = "${alerts.evaluation.interval-ms:900000}",
            initialDelayString = "${alerts.evaluation.initial-delay-ms:60000}"
    )
    public void runScheduledEvaluation() {
        log.info("Scheduler de evaluación disparado");
        try {
            EvaluationSummaryResponse summary = alertEvaluationService.runEvaluation();
            log.info("Scheduler de evaluación finalizado: {}", summary);
        } catch (RuntimeException ex) {
            log.error("La evaluación programada falló", ex);
        }
    }
}