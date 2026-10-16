package es.bytescolab.msdashboard.dto.response.client;

import java.util.Map;

public record VehicleFleetSummary(
        long total,
        Map<String, Long> byStatus,
        Map<String, Long> byType
) {
}
