package es.bytescolab.msroutes.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;
    private final String service;

    protected BusinessException(String errorCode, HttpStatus status, String message) {
        this(errorCode, status, message, null);
    }

    protected BusinessException(String errorCode, HttpStatus status, String message, String service) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.service = service;
    }
}
