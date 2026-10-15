package es.bytescolab.msdashboard.dto.response;

import es.bytescolab.msdashboard.enums.DashboardMetric;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "TimeseriesSeries", description = "Serie temporal de una métrica con su unidad")
public record TimeseriesSeries(

        @Schema(description = "Métrica de la serie", example = "DISTANCE_KM",
                allowableValues = {"DISTANCE_KM", "FUEL_LITERS", "FUEL_COST",
                        "CONSUMPTION_L100KM", "MAINTENANCE_COST", "ALERTS_CREATED"})
        DashboardMetric metric,

        @Schema(description = "Unidad del valor (expuesta para facilitar el consumo desde UI)",
                example = "km")
        String unit,

        @Schema(description = "Puntos de la serie temporal ordenados cronológicamente")
        List<TimeseriesPoint> points
) {
}
