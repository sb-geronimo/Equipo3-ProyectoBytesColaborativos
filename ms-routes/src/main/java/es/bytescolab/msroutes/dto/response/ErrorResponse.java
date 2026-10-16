package es.bytescolab.msroutes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErrorResponse", description = "Error estandarizado de la plataforma")
public record ErrorResponse(
        @Schema(description = "Código de error", example = "VALIDATION_ERROR")
        String error,

        @Schema(description = "Mensaje legible del error", example = "La petición contiene campos no válidos")
        String message,

        @Schema(description = "Detalle por campo; solo aparece en VALIDATION_ERROR")
        List<ValidationDetail> details,

        @Schema(description = "Servicio no disponible; solo aparece en SERVICE_UNAVAILABLE", example = "ms-vehicles")
        String service,

        @Schema(description = "Marca de tiempo en UTC", example = "2026-10-05T10:30:00Z")
        Instant timestamp
) {

    @Schema(name = "ErrorDetail", description = "Detalle de un campo no válido")
    public record ValidationDetail(
            @Schema(description = "Campo afectado", example = "vehicleId")
            String field,

            @Schema(description = "Motivo del rechazo", example = "El vehículo es obligatorio")
            String reason) {
    }

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null, null, Instant.now());
    }

    public static ErrorResponse withService(String error, String message, String service) {
        return new ErrorResponse(error, message, null, service, Instant.now());
    }

    public static ErrorResponse withDetails(String error, String message, List<ValidationDetail> details) {
        return new ErrorResponse(error, message, details, null, Instant.now());
    }
}
