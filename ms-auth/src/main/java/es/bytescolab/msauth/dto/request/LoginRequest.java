package es.bytescolab.msauth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales de inicio de sesión")
public record LoginRequest(
        @Schema(example = "carlos@fleetcontrol.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email invalido")
        String email,
        @Schema(example = "FleetPass123")
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
