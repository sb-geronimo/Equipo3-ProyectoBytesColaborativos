package es.bytescolab.msmaintenance.specification;

import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public class MaintenanceOrderSpecification {

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
}
