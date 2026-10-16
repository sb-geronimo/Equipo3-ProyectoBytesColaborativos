package es.bytescolab.msfuel.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@Schema(name = "ErrorResponse", description = "Error estandarizado de la plataforma")
public record ErrorResponse(
        @Schema(description = "Código de error", example = "VALIDATION_ERROR")
        String error,

        @Schema(description = "Mensaje legible del error", example = "La petición contiene campos no válidos")
        String message,

        @Schema(description = "Servicio no disponible; solo aparece en SERVICE_UNAVAILABLE", example = "ms-vehicles")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String service,

        @Schema(description = "Marca de tiempo en UTC", example = "2026-10-05T10:30:00Z")
        Instant timestamp
) {
}
