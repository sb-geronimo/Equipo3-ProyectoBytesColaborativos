package es.bytescolab.msfuel.service.helper;

import es.bytescolab.msfuel.dto.response.FuelStatsPoint;
import es.bytescolab.msfuel.dto.response.FuelStatsTotals;
import es.bytescolab.msfuel.entity.FuelRecordEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FuelTimeSeriesBuilder {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private FuelTimeSeriesBuilder() {
    }

    public static LocalDate periodStart(LocalDate date, String granularity) {
        return switch (granularity) {
            case "DAY" -> date;
            case "WEEK" -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "MONTH" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    public static List<LocalDate> fillPeriods(LocalDate from, LocalDate to, String granularity) {
        List<LocalDate> periods = new ArrayList<>();
        LocalDate cursor = periodStart(from, granularity);
        LocalDate end = periodStart(to, granularity);
        while (!cursor.isAfter(end)) {
            periods.add(cursor);
            cursor = advancePeriod(cursor, granularity);
        }
        return periods;
    }

    public static FuelStatsTotals computeTotals(List<FuelRecordEntity> refuels) {
        BigDecimal liters = sumLiters(refuels);
        BigDecimal cost = sumCost(refuels);
        BigDecimal avgPrice = liters.signum() > 0
                ? cost.divide(liters, 2, RoundingMode.HALF_UP)
                : ZERO_2;
        BigDecimal avgL100km = averageFullTankConsumption(refuels);
        return new FuelStatsTotals(
                liters.setScale(2, RoundingMode.HALF_UP),
                cost.setScale(2, RoundingMode.HALF_UP),
                avgPrice,
                avgL100km
        );
    }

    public static List<FuelStatsPoint> buildSeries(
            List<FuelRecordEntity> refuels, List<LocalDate> periods, String granularity) {
        Map<LocalDate, List<FuelRecordEntity>> buckets = new HashMap<>();
        for (FuelRecordEntity r : refuels) {
            LocalDate period = periodStart(refuelDateOf(r), granularity);
            buckets.computeIfAbsent(period, k -> new ArrayList<>()).add(r);
        }
        List<FuelStatsPoint> points = new ArrayList<>(periods.size());
        for (LocalDate period : periods) {
            points.add(toStatPoint(period, buckets.getOrDefault(period, List.of())));
        }
        return points;
    }

    private static FuelStatsPoint toStatPoint(LocalDate period, List<FuelRecordEntity> bucket) {
        BigDecimal liters = sumLiters(bucket);
        BigDecimal cost = sumCost(bucket);
        BigDecimal avgPrice = liters.signum() > 0
                ? cost.divide(liters, 2, RoundingMode.HALF_UP)
                : ZERO_2;
        BigDecimal avgL100km = averageFullTankConsumption(bucket);
        return new FuelStatsPoint(
                period,
                liters.setScale(2, RoundingMode.HALF_UP),
                cost.setScale(2, RoundingMode.HALF_UP),
                avgPrice,
                avgL100km
        );
    }

    private static BigDecimal sumLiters(List<FuelRecordEntity> refuels) {
        return refuels.stream()
                .map(FuelRecordEntity::getLiters)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumCost(List<FuelRecordEntity> refuels) {
        return refuels.stream()
                .map(FuelRecordEntity::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal averageFullTankConsumption(List<FuelRecordEntity> refuels) {
        List<BigDecimal> values = refuels.stream()
                .filter(r -> Boolean.TRUE.equals(r.getFullTank()))
                .map(FuelRecordEntity::getConsumptionL100km)
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return ZERO_2;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private static LocalDate advancePeriod(LocalDate cursor, String granularity) {
        return switch (granularity) {
            case "DAY" -> cursor.plusDays(1);
            case "WEEK" -> cursor.plusWeeks(1);
            case "MONTH" -> cursor.plusMonths(1);
            default -> cursor.plusDays(1);
        };
    }

    private static LocalDate refuelDateOf(FuelRecordEntity entity) {
        Instant refueledAt = entity.getRefueledAt();
        return refueledAt == null
                ? null
                : LocalDate.ofInstant(refueledAt, ZoneOffset.UTC);
    }
}
