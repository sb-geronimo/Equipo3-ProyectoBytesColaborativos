package es.bytescolab.msvehicles.service;

import es.bytescolab.msvehicles.dto.request.CreateVehicleRequest;
import es.bytescolab.msvehicles.dto.request.StatusVehicleRequest;
import es.bytescolab.msvehicles.dto.request.UpdateVehicleRequest;
import es.bytescolab.msvehicles.dto.response.PageResponse;
import es.bytescolab.msvehicles.dto.response.VehicleDetailsResponse;
import es.bytescolab.msvehicles.dto.response.VehicleSummaryResponse;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VehicleService {
    PageResponse<VehicleSummaryResponse> findVehicles(
            Pageable pageable,
            VehicleStatus status,
            VehicleType type,
            String plate
    );

    VehicleDetailsResponse createVehicle(CreateVehicleRequest request);

    VehicleDetailsResponse findVehicleById(UUID vehicleId);

    VehicleSummaryResponse updateVehicle(UUID vehicleId, UpdateVehicleRequest request);

    VehicleSummaryResponse updateStatusVehicle(UUID vehicleId, StatusVehicleRequest request);
}
