package es.bytescolab.msalerts.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@Schema(name = "ErrorResponse", description = "Error estandarizado de la plataforma")
public record ErrorResponse(
        @Schema(description = "Código de error", example = "ALERT_NOT_FOUND")
        String error,

        @Schema(description = "Mensaje legible del error", example = "Alerta no encontrada")
        String message,

        @Schema(description = "Marca de tiempo en UTC", example = "2026-10-05T10:30:00Z")
        Instant timestamp
) {
}
