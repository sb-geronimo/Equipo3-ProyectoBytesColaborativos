package es.bytescolab.msauth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
@Schema(description = "Contrato común de errores")
public record ErrorResponse(
        @Schema(description = "Código del error", example = "USER_ALREADY_EXISTS")
        String error,
        @Schema(example = "Ya existe un usuario con este email")
        String message,
        @Schema(description = "Detalle por campo, solo en VALIDATION_ERROR")
        List<Detail> details,
        @Schema(example = "2026-10-05T10:30:00Z")
        Instant timestamp
) {
    @Builder
    public record Detail(
            @Schema(example = "password")
            String field,
            @Schema(example = "La contraseña debe contener al menos una mayúscula y un número")
            String reason) {
    }
}
