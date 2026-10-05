package es.bytescolab.msdrivers.dto.response;

import es.bytescolab.msdrivers.enums.DriverStatus;
import es.bytescolab.msdrivers.enums.LicenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Resumen de un conductor (usado en el listado paginado)")
public record DriverSummaryResponse(

        @Schema(description = "Identificador único del conductor",
                example = "3b2d8f10-7c4a-4e55-bla0-5d9e2c7f1a01")
        UUID id,

        @Schema(description = "Nombre completo", example = "Laura Fernández")
        String fullName,

        @Schema(description = "Email de contacto", example = "laura.fernandez@fleetcontrol.com")
        String email,

        @Schema(description = "Teléfono de contacto", example = "+34 600 123 456")
        String phone,

        @Schema(description = "Número de licencia (único)", example = "B-4471923")
        String licenseNumber,

        @Schema(description = "Categoría de la licencia", allowableValues = {"A", "B", "C"}, example = "B")
        LicenseCategory licenseCategory,

        @Schema(description = "Fecha de caducidad de la licencia (YYYY-MM-DD)", example = "2027-03-14")
        LocalDate licenseExpiresAt,

        @Schema(description = "Estado operativo del conductor",
                allowableValues = {"ACTIVE", "ON_LEAVE", "SUSPENDED"},
                example = "ACTIVE")
        DriverStatus status
) {
}