package es.bytescolab.msmaintenance.mapper;

import es.bytescolab.msmaintenance.dto.request.SchedulePlanRequest;
import es.bytescolab.msmaintenance.dto.response.PlanDetailsResponse;
import es.bytescolab.msmaintenance.dto.response.OrderSummaryResponse;
import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MaintenanceMapper {

    PlanDetailsResponse toPlanDetailsResponse(MaintenancePlan maintenancePlan);

    MaintenancePlan toMaintenancePlan(SchedulePlanRequest createPlanRequest);

    OrderSummaryResponse toOrderSummaryResponse(MaintenanceOrder maintenanceOrder);
}
