package es.bytescolab.msalerts.dto.request;

import es.bytescolab.msalerts.enums.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(name = "UpdateAlertRuleRequest", description = "Modificación de una regla de evaluación")
public record UpdateAlertRuleRequest(

        @NotNull
        @Schema(description = "Indica si la regla debe quedar activa")
        Boolean enabled,

        @NotNull
        @Schema(description = "Nueva severidad que asignará la regla")
        AlertSeverity severity,

        @Schema(description = "Parámetros de la regla (estructura depende del tipo)",
                example = "{\"daysBefore\": 14}")
        Map<String, Object> params
) {
}