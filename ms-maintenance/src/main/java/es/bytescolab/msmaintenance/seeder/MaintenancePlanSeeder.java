package es.bytescolab.msmaintenance.seeder;

import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class MaintenancePlanSeeder {

    private static final int VEHICLE_COUNT = 25;
    private static final int PLANS_PER_VEHICLE = 2;

    private final MaintenancePlanRespository planRepository;

    @Value("${demo.seed:42}")
    private long seed;

    @Value("${demo.days:90}")
    private int demoDays;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void seed() {
        if (planRepository.count() > 0) {
            log.info("Planes: la tabla maintenance_plans ya contiene datos. Se omite el seeder.");
            return;
        }

        log.info("Iniciando seeder demo de ms-maintenance (seed={}, days={})", seed, demoDays);

        Random rng = new Random(seed);
        Faker faker = new Faker(new Locale("es", "ES"), new Random(seed));
        LocalDate today = LocalDate.now();
        MaintenanceType[] types = MaintenanceType.values();
        Map<MaintenanceType, TypeSpec> specsByType = typeSpecs();

        List<MaintenancePlan> plans = new ArrayList<>(VEHICLE_COUNT * PLANS_PER_VEHICLE);

        for (int vehicleIndex = 0; vehicleIndex < VEHICLE_COUNT; vehicleIndex++) {
            UUID vehicleId = deterministicUuid("vehicle-" + vehicleIndex);
            int firstTypeIndex = vehicleIndex % types.length;
            int secondTypeIndex = (vehicleIndex + 2) % types.length;

            plans.add(buildPlan(faker, rng, vehicleId, types[firstTypeIndex], specsByType, today));
            plans.add(buildPlan(faker, rng, vehicleId, types[secondTypeIndex], specsByType, today));
        }

        planRepository.saveAll(plans);

        log.info("Seeder demo de ms-maintenance completado: {} planes de mantenimiento insertados sobre {} vehículos.",
                plans.size(), VEHICLE_COUNT);
    }

    private MaintenancePlan buildPlan(
            Faker faker,
            Random rng,
            UUID vehicleId,
            MaintenanceType type,
            Map<MaintenanceType, TypeSpec> specsByType,
            LocalDate today) {

        TypeSpec spec = specsByType.get(type);
        LocalDate lastDoneAt = today.minusDays(rng.nextInt(demoDays));
        int lastDoneKm = 20_000 + rng.nextInt(80_000);

        LocalDate nextDueAt = spec.intervalDays() != null
                ? lastDoneAt.plusDays(spec.intervalDays())
                : lastDoneAt.plusDays(180);

        Integer nextDueKm = spec.intervalKm() != null
                ? lastDoneKm + spec.intervalKm()
                : null;

        return MaintenancePlan.builder()
                .vehicleId(vehicleId)
                .type(type)
                .intervalKm(spec.intervalKm())
                .intervalDays(spec.intervalDays())
                .lastDoneAt(lastDoneAt)
                .lastDoneKm(lastDoneKm)
                .nextDueAt(nextDueAt)
                .nextDueKm(nextDueKm)
                .active(true)
                .build();
    }

    private static Map<MaintenanceType, TypeSpec> typeSpecs() {
        Map<MaintenanceType, TypeSpec> map = new EnumMap<>(MaintenanceType.class);
        map.put(MaintenanceType.OIL_CHANGE, new TypeSpec(15_000, 365));
        map.put(MaintenanceType.TIRE_ROTATION, new TypeSpec(10_000, 180));
        map.put(MaintenanceType.BRAKE_CHECK, new TypeSpec(20_000, 365));
        map.put(MaintenanceType.GENERAL_INSPECTION, new TypeSpec(30_000, 365));
        map.put(MaintenanceType.LEGAL_INSPECTION, new TypeSpec(null, 730));
        return map;
    }

    private static UUID deterministicUuid(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private record TypeSpec(Integer intervalKm, Integer intervalDays) {
    }
}