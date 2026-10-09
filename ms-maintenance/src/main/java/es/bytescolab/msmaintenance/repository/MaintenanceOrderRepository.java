package es.bytescolab.msmaintenance.repository;

import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceOrderRepository extends JpaRepository<MaintenanceOrder, UUID>,
        JpaSpecificationExecutor<MaintenanceOrder> {

    boolean existsByPlanIdAndStatusIn(UUID planId, List<MaintenanceOrderStatus> status);
}
