package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DashboardFleetBlock", description = "Inventario agregado de la flota aplanado por estado")
public record DashboardFleetBlock(

        @Schema(description = "Total de vehículos en la flota", example = "25")
        long total,

        @Schema(description = "Vehículos disponibles (estado AVAILABLE)", example = "14")
        long available,

        @Schema(description = "Vehículos en uso (estado IN_USE)", example = "8")
        long inUse,

        @Schema(description = "Vehículos en mantenimiento (estado IN_MAINTENANCE)", example = "2")
        long inMaintenance,

        @Schema(description = "Vehículos fuera de servicio (estado OUT_OF_SERVICE)", example = "1")
        long outOfService
) {
}
