package es.bytescolab.msfuel.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.UUID;

public record RefuelFilterRequest(
        UUID vehicle,
        LocalDate from,
        LocalDate to,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public RefuelFilterRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
    }
}
