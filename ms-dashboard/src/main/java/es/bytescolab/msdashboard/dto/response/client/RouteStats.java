package es.bytescolab.msdashboard.dto.response.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RouteStats(
        LocalDate from,
        LocalDate to,
        String granularity,
        RouteStatsTotals totals,
        List<RouteStatsPoint> series
) {

    public record RouteStatsTotals(long routes, BigDecimal distanceKm) {
    }

    public record RouteStatsPoint(
            LocalDate period,
            long routes,
            BigDecimal distanceKm,
            Integer avgDurationMin
    ) {
    }
}
