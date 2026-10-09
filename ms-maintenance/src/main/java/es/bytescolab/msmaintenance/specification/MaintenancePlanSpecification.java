package es.bytescolab.msmaintenance.specification;

import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class MaintenancePlanSpecification {

    public static Specification<MaintenancePlan> hasVehicle(UUID vehicleId) {
        return (root, query, criteriaBuilder) -> {
            if (vehicleId == null) return null;
            return criteriaBuilder.equal(root.get("vehicleId"), vehicleId);
        };
    }

    public static Specification<MaintenancePlan> hasType(MaintenanceType type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) return null;
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }

    public static Specification<MaintenancePlan> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) return null;
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }
}
