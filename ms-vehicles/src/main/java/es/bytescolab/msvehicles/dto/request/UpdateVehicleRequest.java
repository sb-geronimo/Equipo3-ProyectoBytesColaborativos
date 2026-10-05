package es.bytescolab.msvehicles.dto.request;

import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleType;
import jakarta.validation.constraints.*;

public record UpdateVehicleRequest(
        @NotBlank(message = "La marca es obligatoria")
        String make,

        @NotBlank(message = "El modelo es obligatorio")
        String model,

        @NotNull(message = "El año es obligatorio")
        @Min(value = 1990, message = "El año debe ser como mínimo 1990")
        @Max(value = 2027, message = "El año no puede ser mayor a 2027")
        Integer year,

        @NotNull(message = "El tipo de vehículo es obligatorio")
        VehicleType type,

        @NotNull(message = "El tipo de combustible es obligatorio")
        FuelType fuelType,

        @NotNull(message = "La capacidad del tanque es obligatoria")
        @Positive(message = "La capacidad del tanque debe ser mayor a 0")
        Integer tankCapacityL
) {
}
