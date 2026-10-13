package es.bytescolab.msalerts.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(name = "AlertStatsPoint", description = "Punto de la serie temporal de alertas creadas")
public record AlertStatsPoint(

        @Schema(description = "Inicio del periodo (en UTC) al que se refiere el punto",
                example = "2026-09-07")
        LocalDate period,

        @Schema(description = "Número de alertas creadas dentro del periodo", example = "3")
        long created,

        @Schema(description = "Desglose por severidad de las alertas creadas en el periodo")
        AlertStatsBySeverity bySeverity
) {
}
