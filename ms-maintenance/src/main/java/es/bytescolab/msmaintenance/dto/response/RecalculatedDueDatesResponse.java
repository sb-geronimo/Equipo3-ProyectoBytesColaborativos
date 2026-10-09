package es.bytescolab.msmaintenance.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record RecalculatedDueDatesResponse(
        UUID id,
        LocalDate nextDueAt,
        Integer nextDueKm
) {
}
