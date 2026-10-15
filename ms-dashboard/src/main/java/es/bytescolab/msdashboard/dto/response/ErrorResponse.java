package es.bytescolab.msdashboard.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ErrorResponse(
        String error,
        String message,
        List<Detail> details,
        Instant timestamp
) {

    @Builder
    public record Detail(
            String field,
            String reason
    ) {
    }
}
