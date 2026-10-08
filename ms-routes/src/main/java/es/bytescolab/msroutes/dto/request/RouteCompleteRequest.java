package es.bytescolab.msroutes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(name = "RouteCompleteRequest", description = "Petición para completar una ruta en curso")
public record RouteCompleteRequest(

        @NotNull
        @Positive
        @Schema(description = "Distancia real recorrida en km", example = "362.4")
        BigDecimal actualDistanceKm,

        @Size(max = 1000)
        @Schema(description = "Observaciones del trayecto")
        String notes
) {
}
