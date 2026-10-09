package es.bytescolab.msmaintenance.scheduler;

import es.bytescolab.msmaintenance.client.VehicleClient;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusSnapshot;
import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.repository.MaintenanceOrderRepository;
import es.bytescolab.msmaintenance.repository.MaintenancePlanRespository;
import es.bytescolab.msmaintenance.specification.MaintenancePlanSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenaceScheduler {

    private final MaintenancePlanRespository maintenancePlanRespository;
    private final MaintenanceOrderRepository maintenanceOrderRepository;
    private final VehicleClient vehicleClient;

    @Scheduled(cron = "0 0 6 * * *", zone = "UTC")
    @Transactional
    public void generateDueOrders() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Specification<MaintenancePlan> spec = Specification
                .where(MaintenancePlanSpecification.hasActive(true));
        List<MaintenancePlan> activePlans = maintenancePlanRespository.findAll(spec);

        for (MaintenancePlan plan : activePlans) {
            if (maintenanceOrderRepository.existsByPlanIdAndStatusIn(plan.getId(), MaintenanceOrderStatus.openStatuses())) {
                continue;
            }

            boolean dueByDate = plan.getNextDueAt() != null
                                && !plan.getNextDueAt().isAfter(today.plusDays(7));

            boolean dueByKm = false;
            if (plan.getNextDueKm() != null) {
                final StatusSnapshot vehicle;
                try {
                    vehicle = vehicleClient.getVehicle(plan.getVehicleId());
                } catch (Exception e) {
                    log.warn("Scheduler: ms-vehicles no responde para vehicleId={}, se omite plan {}",
                            plan.getVehicleId(), plan.getId());
                    continue;
                }
                if (vehicle.odometerKm() != null) {
                    dueByKm = (plan.getNextDueKm() - vehicle.odometerKm()) <= 500;
                }
            }

            if (dueByDate || dueByKm) {
                MaintenanceOrder order = MaintenanceOrder.builder()
                        .planId(plan.getId())
                        .vehicleId(plan.getVehicleId())
                        .type(plan.getType())
                        .status(MaintenanceOrderStatus.PENDING)
                        .scheduledFor(plan.getNextDueAt() != null ? plan.getNextDueAt() : today)
                        .build();
                maintenanceOrderRepository.save(order);
            }
        }
    }
}
