package es.bytescolab.msalerts.exception;

import java.util.UUID;

public class AlertNotFoundException extends AlertBusinessException {

    public AlertNotFoundException(UUID alertId) {
        super("ALERT_NOT_FOUND", "No existe una alerta con el ID " + alertId);
    }
}