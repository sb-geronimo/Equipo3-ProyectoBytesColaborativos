package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "DashboardUpcomingMaintenanceItem",
        description = "Orden de mantenimiento pendiente próxima a vencer")
public record DashboardUpcomingMaintenanceItem(

        @Schema(description = "Identificador único de la orden")
        UUID orderId,

        @Schema(description = "Identificador del vehículo asociado")
        UUID vehicleId,

        @Schema(description = "Placa del vehículo, obtenida de ms-vehicles",
                example = "4821-KDF")
        String plate,

        @Schema(description = "Tipo de mantenimiento programado",
                example = "OIL_CHANGE")
        String type,

        @Schema(description = "Fecha prevista de la orden (YYYY-MM-DD)",
                example = "2026-10-08")
        LocalDate scheduledFor
) {
}
