package es.bytescolab.msalerts.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "EvaluationSummaryResponse", description = "Resumen de una ronda de evaluación de alertas")
public record EvaluationSummaryResponse(

        @Schema(description = "Número de alertas creadas en esta ronda", example = "3")
        int created,

        @Schema(description = "Número de alertas marcadas como RESOLVED", example = "1")
        int resolved,

        @Schema(description = "Tipos de regla omitidos por dependencia caída",
                example = "[\"HIGH_CONSUMPTION\"]")
        List<String> skipped,

        @Schema(description = "Duración de la evaluación en milisegundos", example = "842")
        long durationMs
) {
}