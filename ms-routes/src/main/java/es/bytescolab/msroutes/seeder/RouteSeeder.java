package es.bytescolab.msroutes.seeder;

import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.RouteStatus;
import es.bytescolab.msroutes.repository.RouteRepository;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class RouteSeeder {

    private static final int VEHICLE_COUNT = 25;
    private static final int DRIVER_COUNT = 20;
    private static final int PLANNED_ROUTES = 3;
    private static final int TARGET_COMPLETED_ROUTES = 1300;
    private static final long BASE_ODOMETER_KM = 40_000L;

    private final RouteRepository routeRepository;

    @Value("${DEMO_SEED:42}")
    private long seed;

    @Value("${DEMO_DAYS:90}")
    private int demoDays;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (routeRepository.count() > 0) {
            log.info("Routes table already populated. Skipping demo seeder.");
            return;
        }

        log.info("Starting demo seeder for ms-routes (seed={}, days={})", seed, demoDays);

        Faker faker = new Faker(new Locale("es", "ES"), new Random(seed));
        Instant now = Instant.now();
        UUID createdBy = deterministicUuid("admin@fleetcontrol.com");

        List<RouteEntity> routes = new ArrayList<>(TARGET_COMPLETED_ROUTES + PLANNED_ROUTES);
        seedCompletedRoutes(routes, faker, now, createdBy);
        seedPlannedRoutes(routes, faker, now, createdBy);

        routeRepository.saveAll(routes);

        log.info("Demo seeder completed: {} routes inserted ({} completed + {} planned).",
                routes.size(), routes.size() - PLANNED_ROUTES, PLANNED_ROUTES);
    }

    private void seedCompletedRoutes(List<RouteEntity> routes, Faker faker, Instant now, UUID createdBy) {
        Random rng = new Random(seed);
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate startDate = today.minusDays(demoDays);

        List<LocalDate> weekdays = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                weekdays.add(d);
            }
        }

        int perDay = Math.max(1, TARGET_COMPLETED_ROUTES / weekdays.size());
        long odometer = BASE_ODOMETER_KM;

        for (LocalDate day : weekdays) {
            int dayCount = Math.max(1, perDay + rng.nextInt(5) - 2);
            for (int i = 0; i < dayCount && routes.size() < TARGET_COMPLETED_ROUTES; i++) {
                int intraDayOffset = (int) ChronoUnit.MINUTES.between(
                        LocalTime.MIN,
                        LocalTime.of(6, 0).plusMinutes(((long) (24 * 60 / (double) dayCount)) * i)
                );
                odometer += 40L + rng.nextInt(120);
                routes.add(buildCompletedRoute(faker, rng, day, now, createdBy, intraDayOffset, odometer));
            }
        }
    }

    private RouteEntity buildCompletedRoute(Faker faker, Random rng, LocalDate day, Instant now,
                                            UUID createdBy, int minuteOfDay, long startOdometer) {
        int hour = minuteOfDay / 60;
        int minute = minuteOfDay % 60;
        LocalTime startTime = LocalTime.of(Math.min(hour, 21), Math.min(minute, 59));
        Instant plannedStart = day.atTime(startTime).toInstant(ZoneOffset.UTC);

        int durationMin = 30 + rng.nextInt(360);
        Instant startedAt = plannedStart.plusSeconds(rng.nextInt(300));
        Instant endedAt = startedAt.plusSeconds((long) durationMin * 60L + rng.nextInt(600));

        int plannedDistance = 20 + rng.nextInt(380);
        int actualDistance = Math.max(1, plannedDistance + rng.nextInt(40) - 10);
        BigDecimal planned = BigDecimal.valueOf(plannedDistance).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actual = BigDecimal.valueOf(actualDistance).setScale(2, RoundingMode.HALF_UP);

        int startOdo = (int) startOdometer;
        int endOdo = startOdo + actualDistance;

        String origin = faker.address().city();
        String destination = faker.address().city();
        while (destination.equalsIgnoreCase(origin)) {
            destination = faker.address().city();
        }

        return RouteEntity.builder()
                .id(UUID.randomUUID())
                .vehicleId(vehicleId(rng.nextInt(VEHICLE_COUNT)))
                .driverId(driverId(rng.nextInt(DRIVER_COUNT)))
                .origin(origin)
                .destination(destination)
                .plannedStart(plannedStart)
                .estimatedDurationMin(durationMin)
                .plannedDistanceKm(planned)
                .status(RouteStatus.COMPLETED)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .startOdometerKm(startOdo)
                .endOdometerKm(endOdo)
                .actualDistanceKm(actual)
                .notes(rng.nextInt(4) == 0 ? faker.lorem().sentence(10) : null)
                .createdBy(createdBy)
                .createdAt(plannedStart)
                .updatedAt(endedAt)
                .isNew(true)
                .build();
    }

    private void seedPlannedRoutes(List<RouteEntity> routes, Faker faker, Instant now, UUID createdBy) {
        Random rng = new Random(seed + 1);
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        for (int i = 0; i < PLANNED_ROUTES; i++) {
            LocalDate day = today.plusDays(1 + rng.nextInt(7));
            while (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                day = day.plusDays(1);
            }
            int hour = 7 + rng.nextInt(8);
            int minute = rng.nextInt(60);
            Instant plannedStart = day.atTime(hour, minute).toInstant(ZoneOffset.UTC);
            int durationMin = 60 + rng.nextInt(300);
            int plannedKm = 50 + rng.nextInt(350);

            String origin = faker.address().city();
            String destination = faker.address().city();
            while (destination.equalsIgnoreCase(origin)) {
                destination = faker.address().city();
            }

            routes.add(RouteEntity.builder()
                    .id(UUID.randomUUID())
                    .vehicleId(vehicleId(rng.nextInt(VEHICLE_COUNT)))
                    .driverId(driverId(rng.nextInt(DRIVER_COUNT)))
                    .origin(origin)
                    .destination(destination)
                    .plannedStart(plannedStart)
                    .estimatedDurationMin(durationMin)
                    .plannedDistanceKm(BigDecimal.valueOf(plannedKm).setScale(2, RoundingMode.HALF_UP))
                    .status(RouteStatus.PLANNED)
                    .createdBy(createdBy)
                    .createdAt(now)
                    .updatedAt(now)
                    .isNew(true)
                    .build());
        }
    }

    private UUID vehicleId(int index) {
        return deterministicUuid("vehicle-" + (index + 1));
    }

    private UUID driverId(int index) {
        return deterministicUuid("driver-" + (index + 1));
    }

    private UUID deterministicUuid(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
