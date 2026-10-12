package es.bytescolab.msfuel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(name = "ConsumptionResponse", description = "Consumo agregado por vehículo en un periodo")
public record ConsumptionResponse(

        @Schema(description = "Fecha inicial del rango en UTC", example = "2026-09-01")
        LocalDate from,

        @Schema(description = "Fecha final del rango en UTC", example = "2026-09-30")
        LocalDate to,

        @Schema(description = "Consumo por vehículo en el rango, ordenado por coste descendente")
        List<VehicleConsumption> vehicles
) {
}
