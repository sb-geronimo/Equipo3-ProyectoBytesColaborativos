package es.bytescolab.msfuel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(name = "FuelStatsTotals", description = "Totales agregados de repostajes en el rango solicitado")
public record FuelStatsTotals(

        @Schema(description = "Litros totales repostados en el rango", example = "3090.0")
        BigDecimal liters,

        @Schema(description = "Coste total en EUR en el rango", example = "5005.80")
        BigDecimal cost,

        @Schema(description = "Precio medio por litro en EUR", example = "1.62")
        BigDecimal avgPricePerLiter,

        @Schema(description = "Consumo medio en L/100 km", example = "11.0")
        BigDecimal avgL100km
) {
}
