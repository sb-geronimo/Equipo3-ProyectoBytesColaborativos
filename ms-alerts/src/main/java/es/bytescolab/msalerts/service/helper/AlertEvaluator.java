package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.entity.AlertRuleEntity;

import java.util.List;

@FunctionalInterface
public interface AlertEvaluator {

    List<AlertCandidate> evaluate(AlertRuleEntity rule);
}