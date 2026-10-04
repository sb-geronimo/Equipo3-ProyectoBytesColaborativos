package es.bytescolab.msvehicles.dto.response;

import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;

import java.util.UUID;

public record VehicleSummaryResponse(
        UUID id,
        String plate,
        String make,
        String model,
        Integer year,
        VehicleType type,
        FuelType fuelType,
        Integer tankCapacityL,
        Integer odometerKm,
        VehicleStatus status
) {
}
