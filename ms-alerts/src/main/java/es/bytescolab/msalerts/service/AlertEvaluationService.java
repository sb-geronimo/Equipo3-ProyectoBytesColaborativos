package es.bytescolab.msalerts.service;

import es.bytescolab.msalerts.dto.response.EvaluationSummaryResponse;

public interface AlertEvaluationService {

    EvaluationSummaryResponse runEvaluation();
}