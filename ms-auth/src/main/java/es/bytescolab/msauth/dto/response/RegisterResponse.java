package es.bytescolab.msauth.dto.response;

import es.bytescolab.msauth.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Usuario creado")
public record RegisterResponse(
        @Schema(example = "3f2a9c1e-7b4d-4e6a-9d2f-1a8b5c6d7e90")
        UUID id,
        @Schema(example = "carlos_ruiz")
        String username,
        @Schema(example = "carlos@fleetcontrol.com")
        String email,
        @Schema(example = "MANAGER")
        UserRole role,
        @Schema(example = "2026-10-05T10:30:00Z")
        Instant createdAt
) {
}
