package es.bytescolab.msfuel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Schema(name = "FuelStatsPoint", description = "Punto de la serie temporal de repostajes")
public record FuelStatsPoint(

        @Schema(description = "Inicio del periodo (en UTC) al que se refiere el punto",
                example = "2026-09-07")
        LocalDate period,

        @Schema(description = "Litros repostados dentro del periodo", example = "984.7")
        BigDecimal liters,

        @Schema(description = "Coste total en EUR dentro del periodo", example = "1595.21")
        BigDecimal cost,

        @Schema(description = "Precio medio por litro en EUR dentro del periodo", example = "1.62")
        BigDecimal avgPricePerLiter,

        @Schema(description = "Consumo medio en L/100 km dentro del periodo", example = "10.9")
        BigDecimal avgL100km
) {
}
