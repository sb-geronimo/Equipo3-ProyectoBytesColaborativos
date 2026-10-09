package es.bytescolab.msroutes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        List<ValidationDetail> details,
        String service,
        Instant timestamp
) {

    public record ValidationDetail(String field, String reason) {
    }

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null, null, Instant.now());
    }

    public static ErrorResponse withService(String error, String message, String service) {
        return new ErrorResponse(error, message, null, service, Instant.now());
    }

    public static ErrorResponse withDetails(String error, String message, List<ValidationDetail> details) {
        return new ErrorResponse(error, message, details, null, Instant.now());
    }
}
