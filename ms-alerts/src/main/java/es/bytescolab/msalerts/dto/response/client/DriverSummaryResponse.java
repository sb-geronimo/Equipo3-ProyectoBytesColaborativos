package es.bytescolab.msalerts.dto.response.client;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "CoversDriverSummary", description = "Resumen de un conductor devuelto por ms-drivers")
public record DriverSummaryResponse(

        @Schema(description = "Identificador único del conductor")
        UUID id,

        @Schema(description = "Nombre completo")
        String fullName,

        @Schema(description = "Número de licencia")
        String licenseNumber,

        @Schema(description = "Fecha de caducidad de la licencia (YYYY-MM-DD)")
        LocalDate licenseExpiresAt
) {
}