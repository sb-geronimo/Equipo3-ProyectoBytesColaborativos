package es.bytescolab.msauth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar un gestor de flota")
public record RegisterRequest(

        @Schema(description = "Nombre de usuario: 3 a 20 caracteres, letras, números y guión bajo",
                example = "carlos_ruiz")
        @NotBlank(message = "El nombre de usuario es necesario")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]{3,20}$",
                message = "Formato de username invalido. Solo se admiten letras, numeros y guión bajo."
        )
        @Size(min = 3, max = 20, message = "El username debe tener entre 3 y 20 caracteres")
        String username,


        @Schema(description = "Email único del usuario", example = "carlos@fleetcontrol.com")
        @NotBlank(message = "El email es necesario")
        @Email(message = "Formato de email invalido")
        String email,

        @Schema(description = "Mínimo 8 caracteres, con al menos una mayúscula y un número",
                example = "FleetPass123")
        @NotBlank(message = "La contraseña es necesaria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contraseña debe contener al menos una mayúscula y un número"
        )
        String password
) {
}
