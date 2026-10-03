package es.bytescolab.msauth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Token de acceso emitido tras el inicio de sesión")
public record AuthResponse(
        @Schema(description = "JWT firmado", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzZjJhLi4uIn0.firma")
        String token,
        @Schema(example = "Bearer")
        String tokenType,
        @Schema(description = "Validez del token en segundos", example = "3600")
        Long expiresIn,
        @Schema(example = "3f2a9c1e-7b4d-4e6a-9d2f-1a8b5c6d7e90")
        String userId,
        @Schema(allowableValues = {"MANAGER", "ADMIN"}, example = "MANAGER")
        String role
) {
}
