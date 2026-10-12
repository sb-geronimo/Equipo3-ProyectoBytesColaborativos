package es.bytescolab.msfuel.exception;

import lombok.Getter;

@Getter
public abstract class FuelBusinessException extends RuntimeException {

    private final String errorCode;

    protected FuelBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
