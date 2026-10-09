package es.bytescolab.msroutes.dto.request;

import es.bytescolab.msroutes.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(name = "VehicleStatusUpdateRequest", description = "Cambio de estado y, opcionalmente, odómetro")
public record VehicleStatusUpdateRequest(

        @Schema(description = "Nuevo estado del vehículo", example = "AVAILABLE")
        VehicleStatus status,

        @PositiveOrZero
        @Schema(description = "Nueva lectura del odómetro en km (opcional, sólo permite aumentar)",
                example = "45572")
        Integer odometerKm
) {
}
