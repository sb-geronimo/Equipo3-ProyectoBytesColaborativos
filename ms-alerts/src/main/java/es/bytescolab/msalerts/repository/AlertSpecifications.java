package es.bytescolab.msalerts.repository;

import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

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