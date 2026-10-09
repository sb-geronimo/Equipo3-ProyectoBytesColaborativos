package es.bytescolab.msmaintenance.client.vehicle.dto;

import es.bytescolab.msmaintenance.client.vehicle.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StatusChangeRequest(
        @NotNull
        VehicleStatus status,
        @Positive
        Integer odometerKm
) {
}
