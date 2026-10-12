package es.bytescolab.msalerts.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "AcknowledgeAlertRequest", description = "Body opcional para reconocer una alerta")
public record AcknowledgeAlertRequest(

        @Size(max = 1000)
        @Schema(description = "Comentario opcional sobre el reconocimiento",
                example = "Cita pedida con el taller para el jueves")
        String comment
) {
}