package es.bytescolab.msfuel.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ErrorResponse(
        String error,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String service,
        Instant timestamp
) {
}
