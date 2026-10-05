package es.bytescolab.msvehicles.exception;

public class InsufficientAuthorityException extends RuntimeException {

    public InsufficientAuthorityException(String message) {
        super(message);
    }
}