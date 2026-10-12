package es.bytescolab.msalerts.exception;

import es.bytescolab.msalerts.enums.AlertStatus;
import java.util.UUID;

public class InvalidAlertStateException extends AlertBusinessException {

    public InvalidAlertStateException(UUID alertId, AlertStatus currentStatus) {
        super("INVALID_ALERT_STATE",
                "La alerta " + alertId + " no se puede reconocer porque está en estado " + currentStatus);
    }
}