package es.bytescolab.msroutes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Respuesta de error cuando una dependencia no responde")
public record ServiceUnavailableErrorResponse(

        @Schema(description = "Codigo de error", example = "SERVICE_UNAVAILABLE")
        String error,

        @Schema(description = "Mensaje legible del error",
                example = "El servicio ms-vehicles no responde")
        String message,

        @Schema(description = "Nombre del servicio dependiente que no responde",
                example = "ms-vehicles")
        String service,

        @Schema(description = "Instante en que se registro el error (UTC, ISO-8601)",
                example = "2026-10-05T10:30:00Z")
        Instant timestamp
) {
}