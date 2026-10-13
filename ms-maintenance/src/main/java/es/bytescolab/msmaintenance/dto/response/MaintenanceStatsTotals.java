package es.bytescolab.msmaintenance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(name = "MaintenanceStatsTotals", description = "Totales agregados para el rango solicitado")
public record MaintenanceStatsTotals(

        @Schema(description = "Número total de órdenes completadas en el rango", example = "9")
        long orders,

        @Schema(description = "Coste total acumulado en el rango, en euros",
                example = "1480.75")
        BigDecimal cost
) {
}