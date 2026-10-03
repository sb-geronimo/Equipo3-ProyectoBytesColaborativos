package es.bytescolab.msgateway.exception.handler;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
@Builder
public class ErrorContext {
    private HttpStatus status;
    private String message;
    private String errorCode;
    private Map<String, Object> metadata;
}
