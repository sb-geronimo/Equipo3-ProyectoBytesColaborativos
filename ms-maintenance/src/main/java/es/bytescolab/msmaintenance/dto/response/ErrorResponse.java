package es.bytescolab.msmaintenance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ErrorResponse(
        String error,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<Detail> details,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String service
) {
    @Builder
    public record Detail(
            String field,
            String reason) {
    }
}