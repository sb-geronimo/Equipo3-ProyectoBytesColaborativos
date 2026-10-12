package es.bytescolab.msfuel.seeder;

import es.bytescolab.msfuel.entity.FuelRecordEntity;
import es.bytescolab.msfuel.enums.FuelType;
import es.bytescolab.msfuel.enums.VehicleType;
import es.bytescolab.msfuel.repository.FuelRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class FuelRecordSeeder {

    private static final int VEHICLE_COUNT = 25;
    private static final int TARGET_REFUELS = 300;
    private static final double CONSUMPTION_VARIANCE = 0.08;
    private static final double HIGH_CONSUMPTION_INCREASE = 0.30;
    private static final int HIGH_CONSUMPTION_DAYS = 30;
    private static final int HIGH_CONSUMPTION_VEHICLE_INDEX_A = 0;
    private static final int HIGH_CONSUMPTION_VEHICLE_INDEX_B = 1;

    private final FuelRecordRepository fuelRecordRepository;

    @Value("${DEMO_SEED:42}")
    private long seed;

    @Value("${DEMO_DAYS:90}")
    private int demoDays;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (fuelRecordRepository.count() > 0) {
            log.info("Combustible: la tabla fuel_records ya contiene datos. Se omite el seeder.");
            return;
        }

        log.info("Iniciando seeder demo de ms-fuel (seed={}, days={})", seed, demoDays);

        Random rng = new Random(seed);
        Faker faker = new Faker(new Locale("es", "ES"), new Random(seed));
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate windowStart = today.minusDays(demoDays);

        Map<UUID, Long> odometerByVehicle = new HashMap<>();
        Map<UUID, Long> lastFullTankKmByVehicle = new HashMap<>();
        Map<UUID, BigDecimal> litersSinceLastFullTankByVehicle = new HashMap<>();

        List<FuelRecordEntity> records = new ArrayList<>(TARGET_REFUELS);
        int perVehicle = Math.max(1, TARGET_REFUELS / VEHICLE_COUNT);

        for (int v = 0; v < VEHICLE_COUNT; v++) {
            UUID vehicleId = deterministicUuid("vehicle-" + v);
            VehicleType vehicleType = VehicleType.values()[v % VehicleType.values().length];
            FuelType fuelType = FuelType.values()[v % FuelType.values().length];
            int tankCapacityL = generateTankCapacity(rng, vehicleType);
            long initialOdometer = 5_000L + rng.nextInt(45_000);

            odometerByVehicle.put(vehicleId, initialOdometer);
            litersSinceLastFullTankByVehicle.put(vehicleId, BigDecimal.ZERO);

            int vehicleRefuels = perVehicle + (rng.nextInt(3) - 1);
            List<Instant> refuelDates = spreadRefuelDates(rng, windowStart, today, vehicleRefuels);

            for (Instant refueledAt : refuelDates) {
                FuelRecordEntity record = buildRefuel(
                        faker, rng, v, vehicleId, vehicleType, fuelType,
                        tankCapacityL, refueledAt, now,
                        odometerByVehicle, lastFullTankKmByVehicle, litersSinceLastFullTankByVehicle
                );
                records.add(record);
            }
        }

        fuelRecordRepository.saveAll(records);

        log.info("Seeder demo de ms-fuel completado: {} repostajes insertados sobre {} vehículos.",
                records.size(), VEHICLE_COUNT);
    }

    private List<Instant> spreadRefuelDates(Random rng, LocalDate windowStart, LocalDate today, int count) {
        List<Instant> dates = new ArrayList<>(count);
        long totalDays = ChronoUnit.DAYS.between(windowStart, today);
        if (totalDays <= 0 || count <= 0) {
            return dates;
        }
        long step = Math.max(1L, totalDays / Math.max(1, count));
        for (int i = 0; i < count; i++) {
            long dayOffset = Math.min(totalDays, step * i + rng.nextInt((int) step));
            Instant base = windowStart.plusDays(dayOffset).atStartOfDay().toInstant(ZoneOffset.UTC);
            long minuteOfDay = rng.nextInt(24 * 60);
            dates.add(base.plus(minuteOfDay, ChronoUnit.MINUTES));
        }
        return dates;
    }

    private FuelRecordEntity buildRefuel(
            Faker faker, Random rng,
            int vehicleIndex,
            UUID vehicleId, VehicleType vehicleType, FuelType fuelType,
            int tankCapacityL, Instant refueledAt, Instant now,
            Map<UUID, Long> odometerByVehicle,
            Map<UUID, Long> lastFullTankKmByVehicle,
            Map<UUID, BigDecimal> litersSinceLastFullTankByVehicle) {

        boolean fullTank = rng.nextInt(4) == 0;

        BigDecimal baseConsumption = BigDecimal.valueOf(baseConsumptionLPer100Km(vehicleType));
        LocalDate refuelDate = LocalDate.ofInstant(refueledAt, ZoneOffset.UTC);
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        boolean recentHighConsumptionVehicle =
                (vehicleIndex == HIGH_CONSUMPTION_VEHICLE_INDEX_A || vehicleIndex == HIGH_CONSUMPTION_VEHICLE_INDEX_B)
                        && ChronoUnit.DAYS.between(refuelDate, today) <= HIGH_CONSUMPTION_DAYS;
        BigDecimal effectiveConsumption = recentHighConsumptionVehicle
                ? baseConsumption.multiply(BigDecimal.valueOf(1.0 + HIGH_CONSUMPTION_INCREASE))
                : applyVariance(baseConsumption, rng, CONSUMPTION_VARIANCE);

        long previousOdometer = odometerByVehicle.get(vehicleId);
        int kmSinceLastRefuel = 40 + rng.nextInt(380);
        long currentOdometer = previousOdometer + kmSinceLastRefuel;
        odometerByVehicle.put(vehicleId, currentOdometer);

        BigDecimal liters = fullTank
                ? BigDecimal.valueOf(tankCapacityL).setScale(2, RoundingMode.HALF_UP)
                : randomPartialLiters(rng, tankCapacityL);

        if (liters.compareTo(BigDecimal.valueOf(tankCapacityL)) > 0) {
            liters = BigDecimal.valueOf(tankCapacityL).setScale(2, RoundingMode.HALF_UP);
            fullTank = true;
        }

        BigDecimal pricePerLiter = pricePerLiterWithWeeklyTrend(rng, refueledAt);
        BigDecimal totalCost = liters.multiply(pricePerLiter).setScale(2, RoundingMode.HALF_UP);

        BigDecimal consumptionL100km = null;
        BigDecimal litersAccum = litersSinceLastFullTankByVehicle.get(vehicleId).add(liters);
        litersSinceLastFullTankByVehicle.put(vehicleId, litersAccum);

        if (fullTank) {
            Long lastFullTankKm = lastFullTankKmByVehicle.get(vehicleId);
            if (lastFullTankKm != null) {
                long kmBetweenFullTanks = currentOdometer - lastFullTankKm;
                if (kmBetweenFullTanks > 0 && litersAccum.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal kmBetween = BigDecimal.valueOf(kmBetweenFullTanks);
                    consumptionL100km = litersAccum
                            .multiply(BigDecimal.valueOf(100))
                            .divide(kmBetween, 2, RoundingMode.HALF_UP);
                }
            } else {
                consumptionL100km = effectiveConsumption;
            }
            lastFullTankKmByVehicle.put(vehicleId, currentOdometer);
            litersSinceLastFullTankByVehicle.put(vehicleId, BigDecimal.ZERO);
        }

        BigDecimal storedConsumption = recentHighConsumptionVehicle
                ? applyVariance(baseConsumption.multiply(BigDecimal.valueOf(1.0 + HIGH_CONSUMPTION_INCREASE)),
                        rng, CONSUMPTION_VARIANCE)
                : (consumptionL100km != null ? consumptionL100km : effectiveConsumption);

        return FuelRecordEntity.builder()
                .id(UUID.randomUUID())
                .vehicleId(vehicleId)
                .refueledAt(refueledAt)
                .fuelType(fuelType)
                .liters(liters)
                .pricePerLiter(pricePerLiter)
                .totalCost(totalCost)
                .odometerKm((int) Math.min(Integer.MAX_VALUE, currentOdometer))
                .fullTank(fullTank)
                .station(faker.options().option("Repsol", "Cepsa", "BP", "Shell", "Galp"))
                .consumptionL100km(storedConsumption)
                .createdAt(refueledAt)
                .isNew(true)
                .build();
    }

    private BigDecimal randomPartialLiters(Random rng, int tankCapacityL) {
        int minLiters = Math.max(5, tankCapacityL / 5);
        int maxLiters = Math.max(minLiters + 1, (int) (tankCapacityL * 0.85));
        int raw = minLiters + rng.nextInt(maxLiters - minLiters + 1);
        BigDecimal fraction = BigDecimal.valueOf(rng.nextInt(100))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(raw).add(fraction).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal pricePerLiterWithWeeklyTrend(Random rng, Instant refueledAt) {
        long daysSinceEpoch = ChronoUnit.DAYS.between(Instant.EPOCH, refueledAt);
        double weeklyTrend = Math.sin(daysSinceEpoch / 7.0 * Math.PI);
        double baseline = 1.60;
        double amplitude = 0.15;
        double noise = (rng.nextInt(1000) - 500) / 10_000.0;
        double raw = baseline + amplitude * weeklyTrend + noise;
        double clamped = Math.max(1.45, Math.min(1.75, raw));
        return BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal applyVariance(BigDecimal base, Random rng, double variance) {
        double factor = 1.0 + (rng.nextInt(2001) - 1000) / 10_000.0 * variance * 2;
        BigDecimal scaled = base.multiply(BigDecimal.valueOf(factor));
        return scaled.setScale(2, RoundingMode.HALF_UP);
    }

    private static double baseConsumptionLPer100Km(VehicleType type) {
        return switch (type) {
            case CAR -> 6.5;
            case VAN -> 10.5;
            case TRUCK -> 27.0;
            case MOTORCYCLE -> 4.2;
        };
    }

    private static int generateTankCapacity(Random rng, VehicleType type) {
        return switch (type) {
            case MOTORCYCLE -> 8 + rng.nextInt(19);
            case CAR -> 40 + rng.nextInt(31);
            case VAN -> 50 + rng.nextInt(51);
            case TRUCK -> 100 + rng.nextInt(401);
        };
    }

    private static UUID deterministicUuid(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
