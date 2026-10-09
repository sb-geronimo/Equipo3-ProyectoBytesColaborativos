package es.bytescolab.msmaintenance.service;

import es.bytescolab.msmaintenance.dto.request.CreatePlanRequest;
import es.bytescolab.msmaintenance.dto.response.CreatePlanResponse;
import es.bytescolab.msmaintenance.enums.MaintenanceType;

import java.util.List;
import java.util.UUID;

public interface MaintenanceService {
    CreatePlanResponse createPlan(CreatePlanRequest request);

    List<CreatePlanResponse> listPlans(UUID vehicleId, MaintenanceType maintenanceType, Boolean active);
}
