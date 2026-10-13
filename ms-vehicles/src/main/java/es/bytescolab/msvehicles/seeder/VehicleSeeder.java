package es.bytescolab.msvehicles.seeder;

import es.bytescolab.msvehicles.entity.Vehicle;
import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class VehicleSeeder {

    private static final int VEHICLE_COUNT = 25;

    private final VehicleRepository vehicleRepository;

    @Value("${demo.seed:42}")
    private long seed;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (vehicleRepository.count() > 0) {
            log.info("Vehículos: la tabla vehicles ya contiene datos. Se omite el seeder.");
            return;
        }

        log.info("Iniciando seeder demo de ms-vehicles (seed={})", seed);

        Faker faker = new Faker(new Locale("es", "ES"), new Random(seed));
        Instant baseInstant = Instant.now();
        List<Vehicle> vehicles = new ArrayList<>(VEHICLE_COUNT);

        for (int i = 0; i < VEHICLE_COUNT; i++) {
            VehicleType type = VehicleType.values()[i % VehicleType.values().length];
            VehicleStatus status = VehicleStatus.values()[i % VehicleStatus.values().length];
            FuelType fuelType = faker.options().option(FuelType.values());
            String make = faker.vehicle().make();
            String model = faker.vehicle().model(make);
            Instant createdAt = baseInstant.minusSeconds((long) (VEHICLE_COUNT - i));

            vehicles.add(Vehicle.builder()
                    .id(deterministicUuid("vehicle-" + i))
                    .plate(generatePlate(i))
                    .make(make)
                    .model(model)
                    .year(faker.number().numberBetween(2020, 2026))
                    .type(type)
                    .fuelType(fuelType)
                    .tankCapacityL(generateTankCapacity(faker, type))
                    .odometerKm(faker.number().numberBetween(5_000, 250_001))
                    .status(status)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());
        }

        vehicleRepository.saveAll(vehicles);

        log.info("Seeder demo de ms-vehicles completado: {} vehículos insertados.", vehicles.size());
    }

    private static UUID deterministicUuid(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static String generatePlate(int index) {
        int number = 1000 + index;
        char letter1 = (char) ('A' + (index / 26) % 26);
        char letter2 = (char) ('A' + index % 26);
        char letter3 = (char) ('A' + (index * 7) % 26);
        return String.format("%04d-%c%c%c", number, letter1, letter2, letter3);
    }

    private static int generateTankCapacity(Faker faker, VehicleType type) {
        return switch (type) {
            case MOTORCYCLE -> faker.number().numberBetween(8, 26);
            case CAR -> faker.number().numberBetween(40, 71);
            case VAN -> faker.number().numberBetween(50, 101);
            case TRUCK -> faker.number().numberBetween(100, 501);
        };
    }
}