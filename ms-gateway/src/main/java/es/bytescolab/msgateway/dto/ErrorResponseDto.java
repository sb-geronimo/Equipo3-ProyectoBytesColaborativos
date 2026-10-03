package es.bytescolab.msgateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        String error,
        String message,
        Map<String, Object> metadata,
        String service,
        Integer retryAfter,
        Instant timestamp
) {
}
