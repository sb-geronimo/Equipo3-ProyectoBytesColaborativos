package es.bytescolab.msvehicles.dto.response;

import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;

import java.time.Instant;
import java.util.UUID;

public record VehicleDetailsResponse(
        UUID id,
        String plate,
        String make,
        String model,
        Integer year,
        VehicleType type,
        FuelType fuelType,
        Integer tankCapacityL,
        Integer odometerKm,
        VehicleStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
