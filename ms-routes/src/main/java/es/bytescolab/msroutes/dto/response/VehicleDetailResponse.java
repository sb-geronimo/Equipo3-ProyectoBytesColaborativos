package es.bytescolab.msroutes.dto.response;

import es.bytescolab.msroutes.enums.VehicleStatus;
import es.bytescolab.msroutes.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@Schema(name = "VehicleDetailResponse", description = "Detalle de un vehículo devuelto por ms-vehicles")
public record VehicleDetailResponse(

        @Schema(description = "Identificador del vehículo")
        UUID id,

        @Schema(description = "Placa o matrícula")
        String plate,

        @Schema(description = "Marca")
        String make,

        @Schema(description = "Modelo")
        String model,

        @Schema(description = "Año de fabricación", example = "2022")
        Integer year,

        @Schema(description = "Tipo de vehículo", example = "VAN")
        VehicleType type,

        @Schema(description = "Tipo de combustible", example = "DIESEL")
        String fuelType,

        @Schema(description = "Capacidad del depósito en litros", example = "80")
        Integer tankCapacityL,

        @Schema(description = "Lectura del odómetro en km", example = "45210")
        Integer odometerKm,

        @Schema(description = "Estado operativo", example = "AVAILABLE")
        VehicleStatus status,

        @Schema(description = "Fecha de alta")
        Instant createdAt,

        @Schema(description = "Fecha de última actualización")
        Instant updatedAt
) {
}
