package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
@Schema(name = "ErrorResponse", description = "Error estandarizado de la plataforma")
public record ErrorResponse(
        @Schema(description = "Código de error", example = "VALIDATION_ERROR")
        String error,

        @Schema(description = "Mensaje legible del error", example = "La petición contiene campos no válidos")
        String message,

        @Schema(description = "Detalle por campo; solo aparece en VALIDATION_ERROR")
        List<Detail> details,

        @Schema(description = "Marca de tiempo en UTC", example = "2026-10-05T10:30:00Z")
        Instant timestamp
) {

    @Builder
    @Schema(name = "ErrorDetail", description = "Detalle de un campo no válido")
    public record Detail(
            @Schema(description = "Campo afectado", example = "vehicleId")
            String field,

            @Schema(description = "Motivo del rechazo", example = "El vehículo es obligatorio")
            String reason
    ) {
    }
}
