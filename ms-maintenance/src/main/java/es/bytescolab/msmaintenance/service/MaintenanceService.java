package es.bytescolab.msmaintenance.service;

import es.bytescolab.msmaintenance.dto.request.SchedulePlanRequest;
import es.bytescolab.msmaintenance.dto.request.CompleteOrderRequest;
import es.bytescolab.msmaintenance.dto.response.*;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MaintenanceService {
    PlanDetailsResponse createPlan(SchedulePlanRequest request);

    List<PlanDetailsResponse> listPlans(UUID vehicleId, MaintenanceType maintenanceType, Boolean active);

    PageResponse<OrderSummaryResponse> findOrders(UUID vehicleId,
                                                  MaintenanceOrderStatus status,
                                                  MaintenanceType maintenanceType,
                                                  LocalDate dueBefore,
                                                  int page,
                                                  int size);


    StartedOrderResponse startOrder(UUID orderId);

    CompletedOrderResponse completeOrder(UUID orderId, CompleteOrderRequest request);
}
