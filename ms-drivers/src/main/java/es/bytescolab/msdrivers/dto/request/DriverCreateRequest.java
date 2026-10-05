package es.bytescolab.msdrivers.dto.request;

import es.bytescolab.msdrivers.enums.LicenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Datos para dar de alta un conductor")
public record DriverCreateRequest(

        @Schema(description = "Nombre completo del conductor", example = "Sara Ibáñez")
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
        String fullName,

        @Schema(description = "Email único del conductor", example = "sara.ibanez@fleetcontrol.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email,

        @Schema(description = "Teléfono de contacto", example = "+34 622 456 789")
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
        String phone,

        @Schema(description = "Número de licencia único", example = "B-5120377")
        @NotBlank(message = "El número de licencia es obligatorio")
        @Size(max = 30, message = "El número de licencia no puede superar los 30 caracteres")
        String licenseNumber,

        @Schema(description = "Categoría de la licencia", allowableValues = {"A", "B", "C"}, example = "B")
        @NotNull(message = "La categoría de la licencia es obligatoria")
        LicenseCategory licenseCategory,

        @Schema(description = "Fecha de caducidad de la licencia (YYYY-MM-DD)", example = "2029-06-30")
        @NotNull(message = "La fecha de caducidad de la licencia es obligatoria")
        @Future(message = "La fecha de caducidad debe ser posterior a la fecha actual")
        LocalDate licenseExpiresAt
) {
}