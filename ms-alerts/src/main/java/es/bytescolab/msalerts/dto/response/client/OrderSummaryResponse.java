package es.bytescolab.msalerts.dto.response.client;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "CoversOrderSummary", description = "Resumen de una orden de mantenimiento devuelto por ms-maintenance")
public record OrderSummaryResponse(

        @Schema(description = "Identificador único de la orden")
        UUID id,

        @Schema(description = "Identificador del plan que originó la orden")
        UUID planId,

        @Schema(description = "Identificador del vehículo asociado")
        UUID vehicleId,

        @Schema(description = "Tipo de mantenimiento")
        String type,

        @Schema(description = "Fecha prevista de la orden (YYYY-MM-DD)")
        LocalDate scheduledFor
) {
}