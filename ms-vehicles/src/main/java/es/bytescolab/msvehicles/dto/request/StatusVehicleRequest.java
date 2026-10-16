package es.bytescolab.msvehicles.dto.request;

import es.bytescolab.msvehicles.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "StatusVehicleRequest", description = "Cambio de estado y, opcionalmente, odómetro")
public record StatusVehicleRequest(
        @Schema(description = "Nuevo estado del vehículo", example = "AVAILABLE")
        @NotNull
        VehicleStatus status,

        @Schema(description = "Nueva lectura del odómetro en km (opcional, solo permite aumentar)", example = "46000")
        @Positive
        Integer odometerKm
) {
}
