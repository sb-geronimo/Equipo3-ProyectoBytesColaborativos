package es.bytescolab.msalerts.exception;

import lombok.Getter;

@Getter
public abstract class AlertBusinessException extends RuntimeException {

    private final String errorCode;

    protected AlertBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}