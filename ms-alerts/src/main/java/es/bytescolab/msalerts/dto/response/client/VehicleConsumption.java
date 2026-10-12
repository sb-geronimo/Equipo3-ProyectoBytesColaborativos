package es.bytescolab.msalerts.dto.response.client;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "CoversVehicleConsumption", description = "Consumo agregado de un vehículo en un periodo")
public record VehicleConsumption(

        @Schema(description = "Identificador del vehículo")
        UUID vehicleId,

        @Schema(description = "Placa del vehículo")
        String plate,

        @Schema(description = "Tipo de vehículo")
        String type,

        @Schema(description = "Litros totales repostados en el periodo")
        BigDecimal liters,

        @Schema(description = "Coste total en EUR en el periodo")
        BigDecimal cost,

        @Schema(description = "Distancia en km recorrida en el periodo")
        Integer distanceKm,

        @Schema(description = "Consumo medio en L/100 km en el periodo")
        BigDecimal avgL100km
) {
}