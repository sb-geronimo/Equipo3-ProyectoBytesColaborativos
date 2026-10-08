package es.bytescolab.msroutes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Schema(name = "RouteStatsPoint", description = "Punto de la serie temporal de rutas completadas")
public record RouteStatsPoint(

        @Schema(description = "Inicio del periodo (en UTC) al que se refiere el punto",
                example = "2026-10-06")
        LocalDate period,

        @Schema(description = "Número de rutas completadas dentro del periodo", example = "12")
        long routes,

        @Schema(description = "Distancia total recorrida dentro del periodo, en km",
                example = "4521.30")
        BigDecimal distanceKm,

        @Schema(description = "Duración media de las rutas del periodo en minutos",
                example = "230")
        Integer avgDurationMin
) {
}
