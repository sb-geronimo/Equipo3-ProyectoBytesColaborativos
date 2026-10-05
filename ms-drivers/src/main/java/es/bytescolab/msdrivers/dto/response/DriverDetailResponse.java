package es.bytescolab.msdrivers.dto.response;

import es.bytescolab.msdrivers.enums.DriverStatus;
import es.bytescolab.msdrivers.enums.LicenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Detalle completo de un conductor, con marcas de auditoría")
public record DriverDetailResponse(

        @Schema(description = "Identificador único del conductor",
                example = "3b2d8f10-7c4a-4e55-bla0-5d9e2c7f1a01")
        UUID id,

        @Schema(description = "Nombre completo", example = "Sara Ibáñez")
        String fullName,

        @Schema(description = "Email de contacto", example = "sara.ibanez@fleetcontrol.com")
        String email,

        @Schema(description = "Teléfono de contacto", example = "+34 622 456 789")
        String phone,

        @Schema(description = "Número de licencia (único)", example = "B-5120377")
        String licenseNumber,

        @Schema(description = "Categoría de la licencia", allowableValues = {"A", "B", "C"}, example = "B")
        LicenseCategory licenseCategory,

        @Schema(description = "Fecha de caducidad de la licencia (YYYY-MM-DD)", example = "2029-06-30")
        LocalDate licenseExpiresAt,

        @Schema(description = "Estado operativo del conductor",
                allowableValues = {"ACTIVE", "ON_LEAVE", "SUSPENDED"},
                example = "ACTIVE")
        DriverStatus status,

        @Schema(description = "Fecha de alta del conductor", example = "2026-10-05T10:30:00Z")
        Instant createdAt,

        @Schema(description = "Fecha de la última modificación", example = "2026-10-05T10:30:00Z")
        Instant updatedAt
) {
}