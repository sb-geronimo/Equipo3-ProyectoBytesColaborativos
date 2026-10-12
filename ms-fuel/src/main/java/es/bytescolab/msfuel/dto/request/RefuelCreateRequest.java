package es.bytescolab.msfuel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "RefuelCreateRequest", description = "Petición para registrar un repostaje")
public record RefuelCreateRequest(

        @NotNull
        @Schema(description = "Identificador del vehículo repostado", example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
        UUID vehicleId,

        @NotNull
        @Schema(description = "Fecha y hora del repostaje en UTC", example = "2026-10-06T12:15:00Z")
        Instant refueledAt,

        @NotNull
        @Positive
        @Digits(integer = 8, fraction = 2)
        @Schema(description = "Litros repostados (debe ser positivo y no superar la capacidad del depósito)",
                example = "58.4")
        BigDecimal liters,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 4, fraction = 2)
        @Schema(description = "Precio por litro en EUR", example = "1.62")
        BigDecimal pricePerLiter,

        @NotNull
        @PositiveOrZero
        @Schema(description = "Lectura del odómetro en km en el momento del repostaje", example = "45572")
        Integer odometerKm,

        @NotNull
        @Schema(description = "Indica si el repostaje llenó el depósito por completo", example = "true")
        Boolean fullTank,

        @Size(max = 255)
        @Schema(description = "Nombre o dirección de la estación de servicio",
                example = "Repsol A-3 km 112")
        String station
) {
}
