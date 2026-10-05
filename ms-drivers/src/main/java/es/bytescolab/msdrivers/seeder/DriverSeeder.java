package es.bytescolab.msdrivers.seeder;

import es.bytescolab.msdrivers.entity.DriverEntity;
import es.bytescolab.msdrivers.enums.DriverStatus;
import es.bytescolab.msdrivers.enums.LicenseCategory;
import es.bytescolab.msdrivers.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DriverSeeder {

    private static final int TOTAL_DRIVERS = 20;
    private static final int NEAR_EXPIRY_COUNT = 3;
    private static final int EXPIRED_COUNT = 1;
    private static final int NEAR_EXPIRY_MAX_DAYS = 15;
    private static final int EXPIRED_MAX_DAYS_AGO = 90;
    private static final int FUTURE_MIN_DAYS = 180;
    private static final int FUTURE_MAX_DAYS = 1825; // ~5 años

    private final DriverRepository driverRepository;

    @Value("${demo.seed:42}")
    private long seed;

    @Value("${demo.days:90}")
    private int days;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (driverRepository.count() > 0) {
            log.info("Seeder omite la creación de conductores: la tabla drivers no está vacía");
            return;
        }

        Faker faker = new Faker(new Locale("es", "ES"), new Random(seed));
        Random rng = new Random(seed);
        LocalDate today = LocalDate.now();
        List<DriverEntity> drivers = new ArrayList<>(TOTAL_DRIVERS);

        for (int i = 0; i < TOTAL_DRIVERS; i++) {
            var driver = this.buildDriver(faker, rng, today, i);
            drivers.add(driver);
        }

        driverRepository.saveAll(drivers);
        log.info("Conductores demo creados: {} (caducados: {}, próximos a caducar: {}, en vigor: {})",
                drivers.size(),
                EXPIRED_COUNT,
                NEAR_EXPIRY_COUNT,
                TOTAL_DRIVERS - EXPIRED_COUNT - NEAR_EXPIRY_COUNT);
    }

    private DriverEntity buildDriver(Faker faker, Random rng, LocalDate today, int i) {
        UUID id = UUID.nameUUIDFromBytes(("driver-" + i).getBytes(StandardCharsets.UTF_8));
        log.debug("Creando conductor demo {} con id {}", i, id);
        String fullName = faker.name().fullName();
        String email = String.format("driver-%02d@fleetcontrol.demo", i);
        String phone = faker.phoneNumber().phoneNumber();
        String licenseNumber = String.format("LIC-%08d", 1_000 + i);
        LicenseCategory category = LicenseCategory.values()[i % LicenseCategory.values().length];
        DriverStatus status = pickStatus(rng);
        LocalDate licenseExpiresAt = pickExpiry(rng, today, i);

        return DriverEntity.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .licenseNumber(licenseNumber)
                .licenseCategory(category)
                .licenseExpiresAt(licenseExpiresAt)
                .status(status)
                .build();
    }

    private DriverStatus pickStatus(Random rng) {
        double r = rng.nextDouble();
        if (r < 0.05) {
            return DriverStatus.SUSPENDED;
        }
        if (r < 0.15) {
            return DriverStatus.ON_LEAVE;
        }
        return DriverStatus.ACTIVE;
    }

    private LocalDate pickExpiry(Random rng, LocalDate today, int i) {
        if (i < EXPIRED_COUNT) {
            int daysAgo = 1 + rng.nextInt(EXPIRED_MAX_DAYS_AGO);
            return today.minusDays(daysAgo);
        }

        if (i < EXPIRED_COUNT + NEAR_EXPIRY_COUNT) {
            int daysAhead = 1 + rng.nextInt(NEAR_EXPIRY_MAX_DAYS);
            return today.plusDays(daysAhead);
        }

        int daysAhead = FUTURE_MIN_DAYS + rng.nextInt(FUTURE_MAX_DAYS - FUTURE_MIN_DAYS + 1);
        return today.plusDays(daysAhead);
    }
}
