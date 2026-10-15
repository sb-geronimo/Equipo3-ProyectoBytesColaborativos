package es.bytescolab.msdashboard.dto.response.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FuelStats(
        LocalDate from,
        LocalDate to,
        String granularity,
        FuelStatsTotals totals,
        List<FuelStatsPoint> series
) {

    public record FuelStatsTotals(
            BigDecimal liters,
            BigDecimal cost,
            BigDecimal avgPricePerLiter,
            BigDecimal avgL100km
    ) {
    }

    public record FuelStatsPoint(
            LocalDate period,
            BigDecimal liters,
            BigDecimal cost,
            BigDecimal avgPricePerLiter,
            BigDecimal avgL100km
    ) {
    }
}
