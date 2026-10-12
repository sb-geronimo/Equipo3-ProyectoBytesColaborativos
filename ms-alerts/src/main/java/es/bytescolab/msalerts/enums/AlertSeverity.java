package es.bytescolab.msalerts.enums;

import java.util.Comparator;

public enum AlertSeverity {

    INFO,
    WARNING,
    CRITICAL;

    private static int severityRank(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case WARNING -> 1;
            case INFO -> 2;
        };
    }

    public static final Comparator<AlertSeverity> RANK = Comparator
            .comparingInt(AlertSeverity::severityRank);
}