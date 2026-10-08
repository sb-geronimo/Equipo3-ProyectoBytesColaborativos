package es.bytescolab.msroutes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(name = "RouteStatsTotals", description = "Totales agregados para el rango solicitado")
public record RouteStatsTotals(

        @Schema(description = "Número total de rutas completadas en el rango", example = "142")
        long routes,

        @Schema(description = "Distancia total recorrida en el rango, expresada en km",
                example = "51230.75")
        BigDecimal distanceKm
) {
}
