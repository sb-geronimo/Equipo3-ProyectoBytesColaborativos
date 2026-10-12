package es.bytescolab.msfuel.service.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FuelConsumptionCalculator {

    private FuelConsumptionCalculator() {
    }

    public static BigDecimal calculateLitersPer100Km(BigDecimal totalLiters, long kmBetweenFills) {
        if (totalLiters == null || totalLiters.signum() <= 0 || kmBetweenFills <= 0) {
            return null;
        }
        return totalLiters
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(kmBetweenFills), 2, RoundingMode.HALF_UP);
    }
}
