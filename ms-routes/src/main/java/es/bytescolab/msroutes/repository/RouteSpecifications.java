package es.bytescolab.msroutes.repository;

import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.RouteStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RouteSpecifications {

    private RouteSpecifications() {
    }

    public static Specification<RouteEntity> withFilters(UUID vehicle,
                                                         UUID driver,
                                                         RouteStatus status,
                                                         Instant from,
                                                         Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(5);
            if (vehicle != null) {
                predicates.add(cb.equal(root.get("vehicleId"), vehicle));
            }
            if (driver != null) {
                predicates.add(cb.equal(root.get("driverId"), driver));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("plannedStart"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("plannedStart"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<RouteEntity> activeOverlapping(UUID vehicleId,
                                                               UUID driverId,
                                                               Instant newEnd) {
        return (root, query, cb) -> cb.and(
                cb.or(
                        cb.equal(root.get("vehicleId"), vehicleId),
                        cb.equal(root.get("driverId"), driverId)
                ),
                root.get("status").in(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS),
                cb.lessThan(root.get("plannedStart"), newEnd)
        );
    }

    public static Specification<RouteEntity> completedInRange(UUID vehicle,
                                                              Instant from,
                                                              Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(4);
            predicates.add(cb.equal(root.get("status"), RouteStatus.COMPLETED));
            predicates.add(cb.greaterThanOrEqualTo(root.get("endedAt"), from));
            predicates.add(cb.lessThan(root.get("endedAt"), to));
            if (vehicle != null) {
                predicates.add(cb.equal(root.get("vehicleId"), vehicle));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
