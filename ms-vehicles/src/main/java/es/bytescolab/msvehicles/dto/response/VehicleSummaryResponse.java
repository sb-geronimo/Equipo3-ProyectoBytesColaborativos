package es.bytescolab.msvehicles.dto.response;

import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "VehicleSummaryResponse", description = "Resumen de un vehículo")
public record VehicleSummaryResponse(
        @Schema(description = "Identificador del vehículo", example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61")
        UUID id,

        @Schema(description = "Placa o matrícula", example = "4821-KDF")
        String plate,

        @Schema(description = "Marca", example = "Ford")
        String make,

        @Schema(description = "Modelo", example = "Transit")
        String model,

        @Schema(description = "Año de fabricación", example = "2022")
        Integer year,

        @Schema(description = "Tipo de vehículo", example = "VAN")
        VehicleType type,

        @Schema(description = "Tipo de combustible", example = "DIESEL")
        FuelType fuelType,

        @Schema(description = "Capacidad del depósito en litros", example = "80")
        Integer tankCapacityL,

        @Schema(description = "Lectura del odómetro en km", example = "45210")
        Integer odometerKm,

        @Schema(description = "Estado operativo", example = "AVAILABLE")
        VehicleStatus status
) {
}
