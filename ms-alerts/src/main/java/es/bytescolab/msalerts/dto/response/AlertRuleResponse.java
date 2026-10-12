package es.bytescolab.msalerts.dto.response;

import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(name = "AlertRuleResponse", description = "Configuración de una regla de evaluación")
public record AlertRuleResponse(

        @Schema(description = "Tipo de alerta al que aplica la regla")
        AlertType type,

        @Schema(description = "Indica si la regla está activa")
        Boolean enabled,

        @Schema(description = "Severidad que se asigna a las alertas generadas")
        AlertSeverity severity,

        @Schema(description = "Parámetros configurables de la regla")
        Map<String, Object> params,

        @Schema(description = "Fecha de la última modificación")
        Instant updatedAt
) {
}