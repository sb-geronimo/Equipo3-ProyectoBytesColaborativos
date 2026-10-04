package es.bytescolab.msvehicles.dto.request;

import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleType;
import jakarta.validation.constraints.*;

public record CreateVehicleRequest(

        @NotBlank(message = "La placa es obligatoria")
        @Pattern(
                regexp = "^[0-9]{4}-[A-Z]{3}$",
                message = "La placa debe tener el formato 1234-ABC"
        )
        String plate,

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
        Integer tankCapacityL,

        @NotNull(message = "El kilometraje es obligatorio")
        @PositiveOrZero(message = "El kilometraje no puede ser negativo")
        Integer odometerKm
) {
}
