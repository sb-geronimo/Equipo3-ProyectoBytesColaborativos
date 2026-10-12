package es.bytescolab.msalerts.dto.response.client;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(name = "CoversConsumptionResponse", description = "Consumo agregado por vehículo devuelto por ms-fuel")
public record ConsumptionResponse(

        @Schema(description = "Fecha inicial del rango en UTC")
        LocalDate from,

        @Schema(description = "Fecha final del rango en UTC")
        LocalDate to,

        @Schema(description = "Listado de vehículos con consumo en el rango solicitado")
        List<VehicleConsumption> vehicles
) {
}