package es.bytescolab.msgateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErrorResponse", description = "Error estandarizado de la plataforma")
public record ErrorResponseDto(
        @Schema(description = "Código de error", example = "RATE_LIMIT_EXCEEDED")
        String error,

        @Schema(description = "Mensaje legible del error", example = "Demasiadas peticiones. Límite: 60 req/min")
        String message,

        @Schema(description = "Metadatos adicionales del error")
        Map<String, Object> metadata,

        @Schema(description = "Servicio no disponible; solo aparece en SERVICE_UNAVAILABLE", example = "ms-fuel")
        String service,

        @Schema(description = "Segundos para reintentar; solo aparece en RATE_LIMIT_EXCEEDED", example = "30")
        Integer retryAfter,

        @Schema(description = "Marca de tiempo en UTC", example = "2026-10-05T10:30:00Z")
        Instant timestamp
) {
}
