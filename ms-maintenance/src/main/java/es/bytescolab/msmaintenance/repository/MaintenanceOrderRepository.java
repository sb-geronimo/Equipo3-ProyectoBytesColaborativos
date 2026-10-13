package es.bytescolab.msmaintenance.repository;

import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.specification.MaintenanceOrderSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceOrderRepository extends JpaRepository<MaintenanceOrder, UUID>,
        JpaSpecificationExecutor<MaintenanceOrder> {

    boolean existsByPlanIdAndStatusIn(UUID planId, List<MaintenanceOrderStatus> status);

    default List<MaintenanceOrder> findCompletedInRange(UUID vehicle, Instant from, Instant to) {
        Specification<MaintenanceOrder> spec = Specification
                .where(MaintenanceOrderSpecification.completedInRange(vehicle, from, to));
        return this.findAll(spec);
    }

    default long countOverduePending(UUID vehicle, LocalDate today) {
        Specification<MaintenanceOrder> spec = Specification
                .where(MaintenanceOrderSpecification.hasStatus(MaintenanceOrderStatus.PENDING))
                .and(MaintenanceOrderSpecification.hasVehicle(vehicle))
                .and(MaintenanceOrderSpecification.scheduledStrictlyBefore(today));
        return this.count(spec);
    }

    default long countDueWithinSevenDays(UUID vehicle, LocalDate from, LocalDate to) {
        Specification<MaintenanceOrder> spec = Specification
                .where(MaintenanceOrderSpecification.hasStatus(MaintenanceOrderStatus.PENDING))
                .and(MaintenanceOrderSpecification.hasVehicle(vehicle))
                .and(MaintenanceOrderSpecification.scheduledBetweenInclusive(from, to));
        return this.count(spec);
    }
}