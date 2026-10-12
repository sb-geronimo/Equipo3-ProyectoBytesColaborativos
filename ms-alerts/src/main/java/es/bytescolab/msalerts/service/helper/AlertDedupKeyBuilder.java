package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.enums.AlertType;

import java.util.UUID;

public final class AlertDedupKeyBuilder {

    private AlertDedupKeyBuilder() {
    }

    public static String build(AlertType type, UUID entityId) {
        return type.name() + ":" + entityId;
    }
}