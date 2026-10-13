package es.bytescolab.msmaintenance.seeder;

import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.repository.MaintenanceOrderRepository;
import es.bytescolab.msmaintenance.repository.MaintenancePlanRespository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
import java.util.Comparator;
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
public class MaintenanceOrderSeeder {

    private static final int VEHICLE_COUNT = 25;
    private static final int TOTAL_PLANS = 50;
    private static final int COMPLETED_PLAN_COUNT = 34;
    private static final int PENDING_UPCOMING_PLAN_COUNT = 5;
    private static final int PENDING_OVERDUE_PLAN_COUNT = 1;
    private static final int MAX_COMPLETED_HOURS_BEFORE_FINISH = 6;
    private static final int MIN_COMPLETED_HOURS_BEFORE_FINISH = 1;
    private static final int COMPLETED_START_HOUR = 8;
    private static final int COMPLETED_END_HOUR = 15;
    private static final int PENDING_UPCOMING_MAX_DAYS = 7;
    private static final int PENDING_OVERDUE_MAX_DAYS = 30;
    private static final int MIN_COST_EUR = 50;
    private static final int MAX_COST_EUR = 500;
    private static final int ODOMETER_SPAN_KM = 15_000;
    private static final String NOTES_NO_INCIDENTS = "Sin incidencias";
    private static final double NOTES_PROBABILITY = 0.30;

    private static final List<String> WORKSHOPS = List.of(
            "Taller Central Madrid",
            "Taller Norte Barcelona",
            "Taller Valencia",
            "Taller Sevilla",
            "RapidMot Sevilla"
    );

    private final MaintenanceOrderRepository orderRepository;
    private final MaintenancePlanRespository planRepository;

    @Value("${demo.seed:42}")
    private long seed;

    @Value("${demo.days:90}")
    private int demoDays;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void seed() {
        if (orderRepository.count() > 0) {
            log.info("Órdenes: la tabla maintenance_orders ya contiene datos. Se omite el seeder.");
            return;
        }

        List<MaintenancePlan> plans = planRepository.findAll();
        if (plans.isEmpty()) {
            log.warn("Órdenes: no hay planes de mantenimiento. Se omite el seeder de órdenes.");
            return;
        }

        log.info("Iniciando seeder de órdenes de mantenimiento sobre {} planes (seed={}, days={})",
                plans.size(), seed, demoDays);

        List<MaintenancePlan> orderedPlans = sortPlansDeterministically(plans);

        Random rng = new Random(seed);
        Faker faker = new Faker(new Locale("es", "ES"), new Random(seed));
        LocalDate today = LocalDate.now();

        List<MaintenanceOrder> orders = new ArrayList<>(TOTAL_PLANS);
        int completedCount = 0;
        int pendingUpcomingCount = 0;
        int pendingOverdueCount = 0;

        for (int index = 0; index < orderedPlans.size(); index++) {
            MaintenancePlan plan = orderedPlans.get(index);
            if (index < COMPLETED_PLAN_COUNT) {
                orders.add(buildCompletedOrder(plan, rng, faker, today));
                completedCount++;
            } else if (index < COMPLETED_PLAN_COUNT + PENDING_UPCOMING_PLAN_COUNT) {
                orders.add(buildPendingUpcomingOrder(plan, rng));
                pendingUpcomingCount++;
            } else if (index < COMPLETED_PLAN_COUNT + PENDING_UPCOMING_PLAN_COUNT + PENDING_OVERDUE_PLAN_COUNT) {
                orders.add(buildPendingOverdueOrder(plan, rng, today));
                pendingOverdueCount++;
            }
        }

        orderRepository.saveAll(orders);

        log.info("Seeder demo de órdenes completado: {} órdenes insertadas (completadas: {}, pendientes próximas: {}, pendientes vencidas: {}).",
                orders.size(), completedCount, pendingUpcomingCount, pendingOverdueCount);
    }

    private List<MaintenancePlan> sortPlansDeterministically(List<MaintenancePlan> plans) {
        Map<UUID, Integer> vehicleIndexById = new HashMap<>(VEHICLE_COUNT);
        for (int i = 0; i < VEHICLE_COUNT; i++) {
            vehicleIndexById.put(deterministicUuid("vehicle-" + i), i);
        }

        List<MaintenancePlan> sorted = new ArrayList<>(plans);
        sorted.sort(Comparator
                .comparing((MaintenancePlan plan) -> vehicleIndexById.getOrDefault(plan.getVehicleId(), VEHICLE_COUNT))
                .thenComparingInt(plan -> plan.getType().ordinal()));
        return sorted;
    }

    private MaintenanceOrder buildCompletedOrder(
            MaintenancePlan plan, Random rng, Faker faker, LocalDate today) {

        LocalDate scheduledFor = today.minusDays(rng.nextInt(demoDays));
        Instant completedAt = scheduledFor
                .atTime(COMPLETED_START_HOUR + rng.nextInt(COMPLETED_END_HOUR - COMPLETED_START_HOUR + 1),
                        rng.nextInt(60))
                .toInstant(ZoneOffset.UTC);
        Instant startedAt = completedAt.minus(
                MIN_COMPLETED_HOURS_BEFORE_FINISH + rng.nextInt(MAX_COMPLETED_HOURS_BEFORE_FINISH),
                ChronoUnit.HOURS);
        int odometerKm = plan.getLastDoneKm() + rng.nextInt(ODOMETER_SPAN_KM + 1);
        BigDecimal cost = BigDecimal.valueOf(MIN_COST_EUR + rng.nextInt(MAX_COST_EUR - MIN_COST_EUR + 1))
                .setScale(2, RoundingMode.HALF_UP);
        String workshop = WORKSHOPS.get(rng.nextInt(WORKSHOPS.size()));
        String notes = rng.nextDouble() < NOTES_PROBABILITY ? NOTES_NO_INCIDENTS : null;

        return MaintenanceOrder.builder()
                .planId(plan.getId())
                .vehicleId(plan.getVehicleId())
                .type(plan.getType())
                .status(MaintenanceOrderStatus.COMPLETED)
                .scheduledFor(scheduledFor)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .odometerKm(odometerKm)
                .cost(cost)
                .workshop(workshop)
                .notes(notes)
                .build();
    }

    private MaintenanceOrder buildPendingUpcomingOrder(MaintenancePlan plan, Random rng) {
        LocalDate scheduledFor = LocalDate.now().plusDays(rng.nextInt(PENDING_UPCOMING_MAX_DAYS + 1));

        return MaintenanceOrder.builder()
                .planId(plan.getId())
                .vehicleId(plan.getVehicleId())
                .type(plan.getType())
                .status(MaintenanceOrderStatus.PENDING)
                .scheduledFor(scheduledFor)
                .build();
    }

    private MaintenanceOrder buildPendingOverdueOrder(MaintenancePlan plan, Random rng, LocalDate today) {
        LocalDate scheduledFor = today.minusDays(1 + rng.nextInt(PENDING_OVERDUE_MAX_DAYS));

        return MaintenanceOrder.builder()
                .planId(plan.getId())
                .vehicleId(plan.getVehicleId())
                .type(plan.getType())
                .status(MaintenanceOrderStatus.PENDING)
                .scheduledFor(scheduledFor)
                .build();
    }

    private static UUID deterministicUuid(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}