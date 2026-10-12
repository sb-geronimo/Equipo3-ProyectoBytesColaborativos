package es.bytescolab.msfuel.repository;

import es.bytescolab.msfuel.entity.FuelRecordEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class FuelRecordSpecifications {

    private FuelRecordSpecifications() {
    }

    public static Specification<FuelRecordEntity> inRange(
            UUID vehicle, Instant fromInclusive, Instant toExclusive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(3);
            if (vehicle != null) {
                predicates.add(cb.equal(root.get("vehicleId"), vehicle));
            }
            if (fromInclusive != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("refueledAt"), fromInclusive));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("refueledAt"), toExclusive));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<FuelRecordEntity> previousRefuelBefore(UUID vehicleId, Instant before) {
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(before, "before must not be null");
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("vehicleId"), vehicleId),
                cb.lessThan(root.get("refueledAt"), before)
        );
    }

    public static Specification<FuelRecordEntity> lastFullTankAtOrBefore(UUID vehicleId, Instant atOrBefore) {
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(atOrBefore, "atOrBefore must not be null");
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("vehicleId"), vehicleId),
                cb.equal(root.get("fullTank"), Boolean.TRUE),
                cb.lessThanOrEqualTo(root.get("refueledAt"), atOrBefore)
        );
    }

    public static Specification<FuelRecordEntity> refuelsStrictlyBetween(
            UUID vehicleId, Instant fromExclusive, Instant toInclusive) {
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(fromExclusive, "fromExclusive must not be null");
        Objects.requireNonNull(toInclusive, "toInclusive must not be null");
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("vehicleId"), vehicleId),
                cb.greaterThan(root.get("refueledAt"), fromExclusive),
                cb.lessThanOrEqualTo(root.get("refueledAt"), toInclusive)
        );
    }
}
