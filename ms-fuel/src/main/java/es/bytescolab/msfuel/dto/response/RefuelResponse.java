package es.bytescolab.msfuel.dto.response;

import es.bytescolab.msfuel.enums.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@Schema(name = "RefuelResponse", description = "Detalle de un repostaje")
public record RefuelResponse(

        @Schema(description = "Identificador del repostaje", example = "b7a3f9e2-4c18-4d5a-9e60-3f1d8c2a7b01")
        UUID id,

        @Schema(description = "Identificador del vehículo repostado",
                example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
        UUID vehicleId,

        @Schema(description = "Fecha y hora del repostaje en UTC", example = "2026-10-06T12:15:00Z")
        Instant refueledAt,

        @Schema(description = "Tipo de combustible, tomado del vehículo", example = "DIESEL")
        FuelType fuelType,

        @Schema(description = "Litros repostados", example = "58.4")
        BigDecimal liters,

        @Schema(description = "Precio por litro en EUR", example = "1.62")
        BigDecimal pricePerLiter,

        @Schema(description = "Coste total del repostaje en EUR", example = "94.61")
        BigDecimal totalCost,

        @Schema(description = "Lectura del odómetro en km en el momento del repostaje", example = "45572")
        Integer odometerKm,

        @Schema(description = "Indica si el repostaje llenó el depósito por completo", example = "true")
        Boolean fullTank,

        @Schema(description = "Nombre o dirección de la estación de servicio",
                example = "Repsol A-3 km 112")
        String station,

        @Schema(description = "Consumo calculado en L/100 km (nulo si no hay un repostaje con depósito "
                + "lleno anterior)", example = "11.3")
        BigDecimal consumptionL100km
) {
}
