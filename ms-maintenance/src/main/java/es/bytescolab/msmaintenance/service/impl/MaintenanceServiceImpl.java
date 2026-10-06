package es.bytescolab.msmaintenance.service.impl;

import es.bytescolab.msmaintenance.client.VehicleClient;
import es.bytescolab.msmaintenance.dto.internal.VehicleResponse;
import es.bytescolab.msmaintenance.dto.request.CreatePlanRequest;
import es.bytescolab.msmaintenance.dto.response.CreatePlanResponse;
import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import es.bytescolab.msmaintenance.exception.PlanAreadyExists;
import es.bytescolab.msmaintenance.exception.VehicleNotFound;
import es.bytescolab.msmaintenance.mapper.MaintenanceMapper;
import es.bytescolab.msmaintenance.repository.MaintenancePlanRespository;
import es.bytescolab.msmaintenance.service.MaintenanceService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {


    private final MaintenancePlanRespository maintenancePlanRespository;
    private final MaintenanceMapper maintenanceMapper;
    private final VehicleClient vehicleClient;

    @Override
    @Transactional
    public CreatePlanResponse createPlan(CreatePlanRequest request) {
        final VehicleResponse vehicle;
        try {
            vehicle = vehicleClient.getVehicle(request.vehicleId());
        } catch (FeignException.NotFound e) {
            throw new VehicleNotFound("No existe un vehículo con el ID proporcionado");
        }

        if (maintenancePlanRespository.existsByVehicleIdAndTypeAndActiveTrue(
                request.vehicleId(), request.type())) {
            throw new PlanAreadyExists(
                    "El vehículo ya tiene un plan activo de este tipo de mantenimiento");
        }

        LocalDate lastDoneAt = request.lastDoneAt() != null
                ? request.lastDoneAt()
                : LocalDate.now(ZoneOffset.UTC);

        Integer lastDoneKm = request.lastDoneKm() != null
                ? request.lastDoneKm()
                : vehicle.odometerKm();

        LocalDate nextDueAt = request.intervalDays() != null
                ? lastDoneAt.plusDays(request.intervalDays())
                : null;

        Integer nextDueKm = request.intervalKm() != null
                ? lastDoneKm + request.intervalKm()
                : null;

        MaintenancePlan toSave = maintenanceMapper.toMaintenancePlan(request);
        toSave.setLastDoneAt(lastDoneAt);
        toSave.setLastDoneKm(lastDoneKm);
        toSave.setNextDueAt(nextDueAt);
        toSave.setNextDueKm(nextDueKm);
        toSave.setActive(true);

        MaintenancePlan saved = maintenancePlanRespository.save(toSave);
        return maintenanceMapper.toCreatePlanResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreatePlanResponse> listPlans(UUID vehicleId, MaintenanceType maintenanceType, Boolean active) {
        return maintenancePlanRespository.search(vehicleId, maintenanceType, active).stream()
                .map(maintenanceMapper::toCreatePlanResponse)
                .toList();
    }
}
