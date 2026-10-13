package es.bytescolab.msalerts.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(name = "AlertStatsTotals", description = "Totales agregados para el rango solicitado")
public record AlertStatsTotals(

        @Schema(description = "Número total de alertas creadas en el rango", example = "11")
        long created
) {
}
