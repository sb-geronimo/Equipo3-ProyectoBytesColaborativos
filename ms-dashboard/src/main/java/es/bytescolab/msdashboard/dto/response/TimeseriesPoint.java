package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "TimeseriesPoint", description = "Punto de una serie temporal con periodo y valor numérico")
public record TimeseriesPoint(

        @Schema(description = "Inicio del periodo (en UTC) al que se refiere el punto",
                example = "2026-10-06")
        LocalDate period,

        @Schema(description = "Valor numérico del punto (la unidad está en la serie padre)",
                example = "4521.30")
        BigDecimal value
) {
}
