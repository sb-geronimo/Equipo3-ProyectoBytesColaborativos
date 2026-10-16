package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "RankingItem", description = "Vehículo en el ranking con su posición y valor")
public record RankingItem(

        @Schema(description = "Posición en el ranking (1 = mejor)", example = "1")
        int rank,

        @Schema(description = "Identificador del vehículo")
        UUID vehicleId,

        @Schema(description = "Placa del vehículo", example = "4821-KDF")
        String plate,

        @Schema(description = "Tipo del vehículo", example = "VAN")
        String type,

        @Schema(description = "Valor numérico por el que se ordena (la unidad la define la métrica)",
                example = "1118.29")
        BigDecimal value
) {
}
