package es.bytescolab.msfuel.dto.response;

import es.bytescolab.msfuel.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Schema(name = "VehicleConsumption", description = "Consumo agregado de un vehículo en el rango solicitado")
public record VehicleConsumption(

        @Schema(description = "Identificador del vehículo",
                example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
        UUID vehicleId,

        @Schema(description = "Placa del vehículo, obtenida de ms-vehicles", example = "4821-KDF")
        String plate,

        @Schema(description = "Tipo del vehículo, obtenido de ms-vehicles", example = "VAN")
        VehicleType type,

        @Schema(description = "Litros totales repostados", example = "690.3")
        BigDecimal liters,

        @Schema(description = "Coste total en EUR", example = "1118.29")
        BigDecimal cost,

        @Schema(description = "Distancia recorrida calculada por diferencia de odómetro", example = "6310")
        Integer distanceKm,

        @Schema(description = "Consumo medio en L/100 km", example = "10.9")
        BigDecimal avgL100km
) {
}
