package es.bytescolab.msalerts.service;

import es.bytescolab.msalerts.dto.request.UpdateAlertRuleRequest;
import es.bytescolab.msalerts.dto.response.AlertRuleResponse;
import es.bytescolab.msalerts.enums.AlertType;

import java.util.List;

public interface AlertRuleService {

    List<AlertRuleResponse> listRules();

    AlertRuleResponse updateRule(AlertType type, UpdateAlertRuleRequest request);
}