package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "DashboardKpisBlock", description = "KPIs operativos del periodo solicitado")
public record DashboardKpisBlock(

        @Schema(description = "Rutas completadas durante el periodo (ms-routes)",
                example = "412")
        Long routesCompleted,

        @Schema(description = "Distancia total recorrida en km durante el periodo (ms-routes)",
                example = "38420.50")
        BigDecimal distanceKm,

        @Schema(description = "Litros totales repostados durante el periodo (ms-fuel)",
                example = "4226.30")
        BigDecimal fuelLiters,

        @Schema(description = "Coste total de combustible en EUR durante el periodo (ms-fuel)",
                example = "6846.61")
        BigDecimal fuelCost,

        @Schema(description = "Consumo medio de la flota en L/100 km durante el periodo (ms-fuel)",
                example = "11.00")
        BigDecimal avgConsumptionL100km,

        @Schema(description = "Coste total de mantenimiento en EUR durante el periodo (ms-maintenance)",
                example = "2140.25")
        BigDecimal maintenanceCost
) {
}
