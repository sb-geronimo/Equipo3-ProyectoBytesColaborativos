package es.bytescolab.msvehicles.exception;

public class InvalidOdometerException extends RuntimeException {

    public InvalidOdometerException(String message) {
        super(message);
    }
}