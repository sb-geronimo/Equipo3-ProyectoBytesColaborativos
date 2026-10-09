package es.bytescolab.msroutes.dto.response;

import es.bytescolab.msroutes.enums.DriverStatus;
import es.bytescolab.msroutes.enums.LicenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Schema(name = "DriverDetailResponse", description = "Detalle de un conductor devuelto por ms-drivers")
public record DriverDetailResponse(

        @Schema(description = "Identificador del conductor")
        UUID id,

        @Schema(description = "Nombre completo")
        String fullName,

        @Schema(description = "Correo electrónico")
        String email,

        @Schema(description = "Teléfono de contacto")
        String phone,

        @Schema(description = "Número de licencia")
        String licenseNumber,

        @Schema(description = "Categoría de la licencia", example = "B")
        LicenseCategory licenseCategory,

        @Schema(description = "Fecha de caducidad de la licencia")
        LocalDate licenseExpiresAt,

        @Schema(description = "Estado operativo del conductor", example = "ACTIVE")
        DriverStatus status
) {
}
