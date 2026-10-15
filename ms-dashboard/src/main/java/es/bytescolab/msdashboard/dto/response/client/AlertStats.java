package es.bytescolab.msdashboard.dto.response.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertStats(
        java.time.LocalDate from,
        java.time.LocalDate to,
        String granularity,
        AlertStatsTotals totals,
        List<AlertStatsPoint> series,
        AlertStatsOpen open
) {

    public record AlertStatsTotals(long created) {
    }

    public record AlertStatsPoint(
            java.time.LocalDate period,
            long created,
            Map<String, Long> bySeverity
    ) {
    }

    public record AlertStatsOpen(
            @JsonValue Map<String, Long> counts
    ) {
        public AlertStatsOpen {
            if (counts == null) {
                counts = new java.util.HashMap<>();
            }
        }
    }
}
