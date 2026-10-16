package es.bytescolab.msvehicles.dto.request;

import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(name = "UpdateVehicleRequest", description = "Datos descriptivos actualizables de un vehículo")
public record UpdateVehicleRequest(
        @Schema(description = "Marca del vehículo", example = "Ford")
        @NotBlank(message = "La marca es obligatoria")
        String make,

        @Schema(description = "Modelo del vehículo", example = "Transit")
        @NotBlank(message = "El modelo es obligatorio")
        String model,

        @Schema(description = "Año de fabricación", example = "2022")
        @NotNull(message = "El año es obligatorio")
        @Min(value = 1990, message = "El año debe ser como mínimo 1990")
        @Max(value = 2027, message = "El año no puede ser mayor a 2027")
        Integer year,

        @Schema(description = "Tipo de vehículo", example = "VAN")
        @NotNull(message = "El tipo de vehículo es obligatorio")
        VehicleType type,

        @Schema(description = "Tipo de combustible", example = "DIESEL")
        @NotNull(message = "El tipo de combustible es obligatorio")
        FuelType fuelType,

        @Schema(description = "Capacidad del depósito en litros", example = "80")
        @NotNull(message = "La capacidad del tanque es obligatoria")
        @Positive(message = "La capacidad del tanque debe ser mayor a 0")
        Integer tankCapacityL
) {
}
