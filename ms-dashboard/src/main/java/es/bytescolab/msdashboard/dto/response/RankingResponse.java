package es.bytescolab.msdashboard.dto.response;

import es.bytescolab.msdashboard.enums.RankingMetric;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "RankingResponse", description = "Ranking de vehículos del periodo solicitado")
public record RankingResponse(

        @Schema(description = "Métrica por la que se ordena el ranking", example = "FUEL_COST",
                allowableValues = {"FUEL_COST", "CONSUMPTION"})
        RankingMetric metric,

        @Schema(description = "Periodo cubierto por el ranking en días (1..365)", example = "30")
        int days,

        @Schema(description = "Número máximo de entradas devueltas (1..20)", example = "5")
        int limit,

        @Schema(description = "Filtro aplicado por tipo de vehículo, si se proporcionó",
                example = "VAN")
        String type,

        @Schema(description = "Unidad del valor devuelto en cada item", example = "EUR")
        String unit,

        @Schema(description = "Ranking ordenado de mayor a menor")
        List<RankingItem> ranking
) {
}
