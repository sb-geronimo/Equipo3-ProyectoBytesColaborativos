package es.bytescolab.msroutes.repository;

import es.bytescolab.msroutes.TestcontainersConfiguration;
import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.RouteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@EnableJpaAuditing
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class RouteRepositoryTest {

    @Autowired
    private RouteRepository routeRepository;

    private final UUID vehicleId = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01");
    private final UUID driverId = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01");
    private final UUID otherVehicleId = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a99");
    private final UUID otherDriverId = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a99");

    private final Instant baseStart = Instant.parse("2026-10-15T07:00:00Z");

    @BeforeEach
    void clean() {
        routeRepository.deleteAll();
    }

    private RouteEntity build(UUID id, UUID vehicle, UUID driver, Instant start,
                              int durationMin, RouteStatus status) {
        return RouteEntity.builder()
                .id(id)
                .vehicleId(vehicle)
                .driverId(driver)
                .origin("Madrid")
                .destination("Valencia")
                .plannedStart(start)
                .estimatedDurationMin(durationMin)
                .plannedDistanceKm(new BigDecimal("355.00"))
                .status(status)
                .build();
    }

    @Test
    @DisplayName("La consulta derivada devuelve candidatos del mismo vehiculo o conductor")
    void derivedQueryReturnsCandidates() {
        RouteEntity planned = routeRepository.save(build(
                UUID.randomUUID(), vehicleId, otherDriverId,
                baseStart, 60, RouteStatus.PLANNED));
        RouteEntity inProgress = routeRepository.save(build(
                UUID.randomUUID(), otherVehicleId, driverId,
                baseStart.plus(Duration.ofMinutes(30)), 90, RouteStatus.IN_PROGRESS));
        RouteEntity completed = routeRepository.save(build(
                UUID.randomUUID(), vehicleId, driverId,
                baseStart.plus(Duration.ofMinutes(180)), 30, RouteStatus.COMPLETED));

        Instant newEnd = baseStart.plus(Duration.ofMinutes(240));
        List<RouteEntity> candidates = routeRepository
                .findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
                        vehicleId, driverId,
                        EnumSet.of(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS),
                        newEnd);

        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().anyMatch(r -> r.getId().equals(planned.getId())));
        assertTrue(candidates.stream().anyMatch(r -> r.getId().equals(inProgress.getId())));
    }

    @Test
    @DisplayName("El predicado JPA reduce los candidatos a los que solapan en Java")
    void overlapPredicateNarrowsCandidates() {
        RouteEntity overlappingVehicle = routeRepository.save(build(
                UUID.randomUUID(), vehicleId, otherDriverId,
                baseStart.plus(Duration.ofMinutes(30)), 120, RouteStatus.PLANNED));
        RouteEntity nonOverlappingVehicle = routeRepository.save(build(
                UUID.randomUUID(), vehicleId, otherDriverId,
                baseStart.plus(Duration.ofHours(10)), 60, RouteStatus.PLANNED));
        RouteEntity overlappingDriver = routeRepository.save(build(
                UUID.randomUUID(), otherVehicleId, driverId,
                baseStart.plus(Duration.ofMinutes(60)), 60, RouteStatus.PLANNED));
        RouteEntity nonOverlappingDriver = routeRepository.save(build(
                UUID.randomUUID(), otherVehicleId, driverId,
                baseStart.plus(Duration.ofHours(20)), 60, RouteStatus.PLANNED));

        Instant plannedStart = baseStart;
        int estimatedDurationMin = 240;
        Instant newEnd = plannedStart.plus(Duration.ofMinutes(estimatedDurationMin));

        List<RouteEntity> candidates = routeRepository
                .findByVehicleIdOrDriverIdAndStatusInAndPlannedStartBefore(
                        vehicleId, driverId,
                        EnumSet.of(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS),
                        newEnd);

        long conflicting = candidates.stream()
                .filter(c -> c.getPlannedStart().isBefore(newEnd)
                        && c.getPlannedStart().plus(Duration.ofMinutes(c.getEstimatedDurationMin()))
                                .isAfter(plannedStart))
                .count();

        assertEquals(2, candidates.size());
        assertEquals(2, conflicting);
        assertTrue(candidates.stream().anyMatch(c -> c.getId().equals(overlappingVehicle.getId())));
        assertTrue(candidates.stream().anyMatch(c -> c.getId().equals(overlappingDriver.getId())));
        assertTrue(candidates.stream().noneMatch(c -> c.getId().equals(nonOverlappingVehicle.getId())));
        assertTrue(candidates.stream().noneMatch(c -> c.getId().equals(nonOverlappingDriver.getId())));
    }

    @Test
    @DisplayName("findById y findAll funcionan contra el repositorio")
    void basicFindOperationsWork() {
        RouteEntity entity = routeRepository.save(build(
                UUID.randomUUID(), vehicleId, driverId,
                baseStart, 60, RouteStatus.PLANNED));

        assertTrue(routeRepository.findById(entity.getId()).isPresent());
        assertEquals(1, routeRepository.findAll().size());
    }

}