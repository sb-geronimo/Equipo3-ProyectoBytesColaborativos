package es.bytescolab.msmaintenance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(name = "CompleteOrderRequest", description = "Datos para completar una orden de mantenimiento")
public record CompleteOrderRequest(

        @Schema(description = "Coste del mantenimiento en EUR", example = "249.99")
        @NotNull
        @PositiveOrZero
        BigDecimal cost,

        @Schema(description = "Lectura del odómetro en km al completar la orden", example = "46230")
        @PositiveOrZero
        Integer odometerKm,

        @Schema(description = "Taller donde se realizó el mantenimiento", example = "Talleres Central")
        @NotBlank
        String workshop,

        @Schema(description = "Notas u observaciones del trabajo", example = "Cambio de aceite y filtro")
        String notes
) {
}
