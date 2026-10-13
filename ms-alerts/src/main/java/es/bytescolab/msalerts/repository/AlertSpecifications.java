package es.bytescolab.msalerts.repository;

import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class AlertSpecifications {

    private AlertSpecifications() {
    }

    public static Specification<AlertEntity> statusIn(Collection<AlertStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<AlertEntity> statusEquals(AlertStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<AlertEntity> createdInRange(Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(2);
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to));
            }
            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<AlertEntity> withFilters(
            AlertStatus status,
            AlertSeverity severity,
            AlertType type,
            UUID vehicleId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(4);
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (vehicleId != null) {
                predicates.add(cb.equal(root.get("vehicleId"), vehicleId));
            }
            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
