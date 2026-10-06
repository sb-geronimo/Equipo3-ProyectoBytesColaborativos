package es.bytescolab.msmaintenance.mapper;

import es.bytescolab.msmaintenance.dto.request.CreatePlanRequest;
import es.bytescolab.msmaintenance.dto.response.CreatePlanResponse;
import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MaintenanceMapper {

    CreatePlanResponse toCreatePlanResponse(MaintenancePlan maintenancePlan);

    MaintenancePlan toMaintenancePlan(CreatePlanRequest createPlanRequest);
}
