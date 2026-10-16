package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertType;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEvaluatorRegistry {

    private final MaintenanceEvaluator maintenanceEvaluator;
    private final LicenseEvaluator licenseEvaluator;
    private final HighConsumptionEvaluator highConsumptionEvaluator;

    private Map<AlertType, AlertEvaluator> mapping;

    private Map<AlertType, AlertEvaluator> mapping() {
        if (mapping == null) {
            Map<AlertType, AlertEvaluator> map = new EnumMap<>(AlertType.class);
            map.put(AlertType.MAINTENANCE_DUE, maintenanceEvaluator);
            map.put(AlertType.MAINTENANCE_OVERDUE, maintenanceEvaluator);
            map.put(AlertType.LICENSE_EXPIRING, licenseEvaluator);
            map.put(AlertType.LICENSE_EXPIRED, licenseEvaluator);
            map.put(AlertType.HIGH_CONSUMPTION, highConsumptionEvaluator);
            this.mapping = map;
        }
        return mapping;
    }

    public EvaluationOutcome run(AlertRuleEntity rule) {
        AlertEvaluator evaluator = mapping().get(rule.getType());
        if (evaluator == null) {
            log.warn("No hay evaluador registrado para la regla {}", rule.getType());
            return new EvaluationOutcome(List.of(), true);
        }
        try {
            List<AlertCandidate> candidates = evaluator.evaluate(rule);
            return new EvaluationOutcome(candidates, false);
        } catch (FeignException ex) {
            log.warn("Dependencia caída al evaluar {}: {}", rule.getType(), ex.getMessage());
            return new EvaluationOutcome(List.of(), true);
        } catch (RuntimeException ex) {
            log.error("Error inesperado al evaluar {}", rule.getType(), ex);
            return new EvaluationOutcome(List.of(), true);
        }
    }

    public record EvaluationOutcome(List<AlertCandidate> candidates, boolean skipped) {
        public EvaluationOutcome {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }

        public static EvaluationOutcome empty() {
            return new EvaluationOutcome(new ArrayList<>(), true);
        }
    }
}