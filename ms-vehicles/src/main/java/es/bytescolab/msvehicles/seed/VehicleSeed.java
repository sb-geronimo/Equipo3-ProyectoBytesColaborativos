package es.bytescolab.msvehicles.seed;

import es.bytescolab.msvehicles.entity.Vehicle;
import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Configuration
@Profile("demo")
@RequiredArgsConstructor
public class VehicleSeed {

    private static final int VEHICLE_COUNT = 25;

    private final VehicleRepository vehicleRepository;

    @Value("${demo.seed}")
    private long demoSeed;

    @Value("${demo.days}")
    private int demoDays;

    @Bean
    public CommandLineRunner seedVehicles() {
        return args -> {
            // Solo generar datos cuando la tabla está vacía.
            if (vehicleRepository.count() > 0) {
                return;
            }

            Faker faker = new Faker(new Random(demoSeed));
            Instant now = Instant.now();

            List<Vehicle> vehicles = new ArrayList<>(VEHICLE_COUNT);

            for (int i = 0; i < VEHICLE_COUNT; i++) {

                VehicleType type = generateType(i);
                FuelType fuelType = generateFuelType(faker);
                VehicleStatus status = generateStatus(i);

                String make = faker.vehicle().make();
                String model = faker.vehicle().model(make);

                Instant createdAt = generateCreatedAt(faker, now);

                Vehicle vehicle = Vehicle.builder()
                        //.id(generateId(i))
                        .plate(generatePlate(i))
                        .make(make)
                        .model(model)
                        .year(generateYear(faker))
                        .type(type)
                        .fuelType(fuelType)
                        .tankCapacityL(generateTankCapacity(faker, type))
                        .odometerKm(generateOdometer(faker))
                        .status(status)
                        .createdAt(createdAt)
                        .updatedAt(createdAt)
                        .build();

                vehicles.add(vehicle);
            }

            vehicleRepository.saveAll(vehicles);
        };
    }

    /**
     * Genera un UUID determinista.
     */
    private UUID generateId(int index) {
        return UUID.nameUUIDFromBytes(
                ("vehicle-" + index)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Genera una placa única y determinista.
     */
    private String generatePlate(int index) {
        int number = 1000 + index;
        char letter1 = (char) ('A' + (index / 26) % 26);
        char letter2 = (char) ('A' + index % 26);
        char letter3 = (char) ('A' + (index * 7) % 26);

        return String.format("%04d-%c%c%c", number, letter1, letter2, letter3);
    }

    /**
     * Distribuye los cuatro tipos de vehículo de forma cíclica.
     */
    private VehicleType generateType(int index) {
        VehicleType[] types = VehicleType.values();
        return types[index % types.length];
    }

    /**
     * Distribuye los cuatro estados de forma cíclica.
     * Esto garantiza que los 25 vehículos cubran todos los estados.
     */
    private VehicleStatus generateStatus(int index) {
        VehicleStatus[] statuses = VehicleStatus.values();
        return statuses[index % statuses.length];
    }

    /**
     * El combustible se selecciona aleatoriamente,
     * pero de forma reproducible gracias a la semilla.
     */
    private FuelType generateFuelType(Faker faker) {
        return faker.options().option(FuelType.values());
    }

    /**
     * Año de fabricación.
     */
    private int generateYear(Faker faker) {
        return faker.number().numberBetween(2020, 2026);
    }

    /**
     * Capacidad del depósito según el tipo de vehículo.
     */
    private int generateTankCapacity(Faker faker, VehicleType type) {
        return switch (type) {
            case MOTORCYCLE -> faker.number().numberBetween(8, 26);

            case CAR -> faker.number().numberBetween(40, 71);

            case VAN -> faker.number().numberBetween(50, 101);

            case TRUCK -> faker.number().numberBetween(100, 501);
        };
    }

    /**
     * Kilometraje acumulado.
     */
    private int generateOdometer(Faker faker) {
        return faker.number().numberBetween(5_000, 250_001);
    }

    /**
     * Fecha de alta dentro de los últimos DEMO_DAYS días.
     */
    private Instant generateCreatedAt(Faker faker, Instant now) {
        int daysAgo = faker.number().numberBetween(0, demoDays + 1);

        return now.minus(daysAgo, ChronoUnit.DAYS);
    }
}