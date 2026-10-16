package es.bytescolab.msvehicles.dto.response;

import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.EnumMap;
import java.util.Map;

@Schema(name = "FleetSummaryResponse", description = "Resumen agregado de la flota de vehículos")
public record FleetSummaryResponse(

        @Schema(description = "Total de vehículos registrados en la flota", example = "25")
        long total,

        @Schema(description = "Conteo de vehículos agrupado por estado",
                example = "{\"AVAILABLE\":14,\"IN_USE\":8,\"IN_MAINTENANCE\":2,\"OUT_OF_SERVICE\":1}")
        Map<VehicleStatus, Long> byStatus,

        @Schema(description = "Conteo de vehículos agrupado por tipo",
                example = "{\"CAR\":9,\"VAN\":10,\"TRUCK\":4,\"MOTORCYCLE\":2}")
        Map<VehicleType, Long> byType
) {
    public FleetSummaryResponse {
        if (byStatus == null) {
            byStatus = mapaEstadosVacio();
        }
        if (byType == null) {
            byType = mapaTiposVacio();
        }
    }

    private static Map<VehicleStatus, Long> mapaEstadosVacio() {
        Map<VehicleStatus, Long> mapa = new EnumMap<>(VehicleStatus.class);
        for (VehicleStatus estado : VehicleStatus.values()) {
            mapa.put(estado, 0L);
        }
        return mapa;
    }

    private static Map<VehicleType, Long> mapaTiposVacio() {
        Map<VehicleType, Long> mapa = new EnumMap<>(VehicleType.class);
        for (VehicleType tipo : VehicleType.values()) {
            mapa.put(tipo, 0L);
        }
        return mapa;
    }
}
