package es.bytescolab.msalerts.exception;

import es.bytescolab.msalerts.enums.AlertType;

public class InvalidAlertRuleParamsException extends AlertBusinessException {

    public InvalidAlertRuleParamsException(AlertType type, String reason) {
        super("VALIDATION_ERROR",
                "Parámetros inválidos para la regla " + type + ": " + reason);
    }
}