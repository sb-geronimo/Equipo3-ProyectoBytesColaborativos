package es.bytescolab.msdashboard.dto.response.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MaintenanceStats(
        LocalDate from,
        LocalDate to,
        String granularity,
        MaintenanceStatsTotals totals,
        List<MaintenanceStatsPoint> series,
        List<MaintenanceStatsByType> byType,
        MaintenanceStatsUpcoming upcoming
) {

    public record MaintenanceStatsTotals(long orders, BigDecimal cost) {
    }

    public record MaintenanceStatsPoint(
            LocalDate period,
            long orders,
            BigDecimal cost
    ) {
    }

    public record MaintenanceStatsByType(
            String type,
            long orders,
            BigDecimal cost
    ) {
    }

    public record MaintenanceStatsUpcoming(long overdue, long dueIn7Days) {
    }
}
