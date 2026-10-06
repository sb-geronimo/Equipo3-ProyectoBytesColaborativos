package es.bytescolab.msvehicles.dto.request;

import es.bytescolab.msvehicles.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StatusVehicleRequest(
        @NotNull
        VehicleStatus status,

        @Positive
        Integer odometerKm
) {
}
