package es.bytescolab.msmaintenance.specification;

import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaintenanceOrderSpecification {

    private MaintenanceOrderSpecification() {
    }

    public static Specification<MaintenanceOrder> hasVehicle(UUID vehicleId) {
        return (root, query, criteriaBuilder) -> {
            if (vehicleId == null) return null;
            return criteriaBuilder.equal(root.get("vehicleId"), vehicleId);
        };
    }

    public static Specification<MaintenanceOrder> hasStatus(MaintenanceOrderStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) return null;
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<MaintenanceOrder> hasType(MaintenanceType type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) return null;
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }

    public static Specification<MaintenanceOrder> dueBeforeOrOn(LocalDate dueBefore) {
        return (root, query, criteriaBuilder) -> {
            if (dueBefore == null) return null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("scheduledFor"), dueBefore);
        };
    }

    public static Specification<MaintenanceOrder> scheduledStrictlyBefore(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) return null;
            return criteriaBuilder.lessThan(root.get("scheduledFor"), date);
        };
    }

    public static Specification<MaintenanceOrder> scheduledBetweenInclusive(LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            if (from == null || to == null) return null;
            return criteriaBuilder.between(root.get("scheduledFor"), from, to);
        };
    }

    public static Specification<MaintenanceOrder> completedAtBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(2);
            predicates.add(cb.greaterThanOrEqualTo(root.get("completedAt"), from));
            predicates.add(cb.lessThan(root.get("completedAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<MaintenanceOrder> completedInRange(UUID vehicle, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(4);
            predicates.add(cb.equal(root.get("status"), MaintenanceOrderStatus.COMPLETED));
            predicates.add(cb.greaterThanOrEqualTo(root.get("completedAt"), from));
            predicates.add(cb.lessThan(root.get("completedAt"), to));
            if (vehicle != null) {
                predicates.add(cb.equal(root.get("vehicleId"), vehicle));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}