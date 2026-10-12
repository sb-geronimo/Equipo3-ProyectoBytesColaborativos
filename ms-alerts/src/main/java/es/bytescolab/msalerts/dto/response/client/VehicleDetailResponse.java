package es.bytescolab.msalerts.dto.response.client;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "VehicleDetailResponse", description = "Detalle de un vehículo devuelto por ms-vehicles")
public record VehicleDetailResponse(

        @Schema(description = "Identificador del vehículo")
        UUID id,

        @Schema(description = "Placa o matrícula")
        String plate
) {
}