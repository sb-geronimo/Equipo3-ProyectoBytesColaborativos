package es.bytescolab.msfuel.dto.response;

import es.bytescolab.msfuel.enums.FuelType;
import es.bytescolab.msfuel.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder
@Schema(name = "VehicleDetailResponse", description = "Detalle de un vehículo devuelto por ms-vehicles")
public record VehicleDetailResponse(

        @Schema(description = "Identificador del vehículo")
        UUID id,

        @Schema(description = "Placa o matrícula")
        String plate,

        @Schema(description = "Tipo de vehículo", example = "VAN")
        VehicleType type,

        @Schema(description = "Tipo de combustible", example = "DIESEL")
        FuelType fuelType,

        @Schema(description = "Capacidad del depósito en litros", example = "80")
        Integer tankCapacityL,

        @Schema(description = "Lectura del odómetro en km", example = "45210")
        Integer odometerKm
) {
}
