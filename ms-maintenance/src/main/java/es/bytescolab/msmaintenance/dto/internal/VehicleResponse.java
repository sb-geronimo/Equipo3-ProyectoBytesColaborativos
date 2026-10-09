package es.bytescolab.msmaintenance.dto.internal;

import java.time.Instant;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String plate,
        String make,
        String model,
        Integer year,
        String type,
        String fuelType,
        Integer tankCapacityL,
        Integer odometerKm,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
