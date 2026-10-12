package es.bytescolab.msalerts.exception;

import es.bytescolab.msalerts.enums.AlertType;

public class AlertRuleNotFoundException extends AlertBusinessException {

    public AlertRuleNotFoundException(AlertType type) {
        super("RULE_NOT_FOUND", "No existe una regla configurada para el tipo " + type);
    }
}