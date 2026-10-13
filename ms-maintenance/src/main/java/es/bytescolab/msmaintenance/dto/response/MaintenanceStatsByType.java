package es.bytescolab.msmaintenance.dto.response;

import es.bytescolab.msmaintenance.enums.MaintenanceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(name = "MaintenanceStatsByType", description = "Agregado por tipo de mantenimiento dentro del rango")
public record MaintenanceStatsByType(

        @Schema(description = "Tipo de mantenimiento", example = "OIL_CHANGE")
        MaintenanceType type,

        @Schema(description = "Número de órdenes completadas de este tipo", example = "4")
        long orders,

        @Schema(description = "Coste total acumulado para este tipo, en euros",
                example = "742.00")
        BigDecimal cost
) {
}