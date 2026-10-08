package es.bytescolab.msroutes.specification;

import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.RouteStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

public class RouteSpecifications {

    public static Specification<RouteEntity> hasVehicle(UUID vehicleId) {
        return (root, query, criteriaBuilder) -> {
            if (vehicleId == null) return null;
            return criteriaBuilder.equal(root.get("vehicleId"), vehicleId);
        };
    }

    public static Specification<RouteEntity> hasDriver(UUID driverId) {
        return (root, query, criteriaBuilder) -> {
            if (driverId == null) return null;
            return criteriaBuilder.equal(root.get("driverId"), driverId);
        };
    }

    public static Specification<RouteEntity> hasStatus(RouteStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) return null;
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<RouteEntity> plannedStartBetween(LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            if (from == null && to == null) return null;

            Instant fromInstant = from == null ? null
                    : from.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant toInstant = to == null ? null
                    : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

            if (fromInstant != null && toInstant != null) {
                return criteriaBuilder.between(root.get("plannedStart"), fromInstant, toInstant);
            }
            if (fromInstant != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("plannedStart"), fromInstant);
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("plannedStart"), toInstant);
        };
    }
}