package es.bytescolab.msmaintenance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "RecalculatedDueDatesResponse", description = "Fechas de vencimiento recalculadas de un plan")
public record RecalculatedDueDatesResponse(
        @Schema(description = "Identificador del plan", example = "9d2f7c10-4b8a-4e55-9c21-7a6d5e4f3b01")
        UUID id,

        @Schema(description = "Próximo vencimiento por fecha", example = "2027-10-16")
        LocalDate nextDueAt,

        @Schema(description = "Próximo vencimiento por kilómetros", example = "60000")
        Integer nextDueKm
) {
}
