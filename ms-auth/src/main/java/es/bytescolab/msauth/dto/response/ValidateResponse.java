package es.bytescolab.msauth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Resultado de la validación de un token")
public record ValidateResponse(
        @Schema(example = "true")
        boolean valid,
        @Schema(example = "3f2a9c1e-7b4d-4e6a-9d2f-1a8b5c6d7e90")
        String userId,
        @Schema(example = "carlos_ruiz")
        String username,
        @Schema(example = "MANAGER")
        String role,
        @Schema(description = "Código de error cuando el token no es válido", example = "TOKEN_EXPIRED")
        String error,
        @Schema(example = "El token ha expirado")
        String message
) {
}
