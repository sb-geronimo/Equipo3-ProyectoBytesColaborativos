package es.bytescolab.msmaintenance.exception;

public class InvalidOrderState extends RuntimeException {
    public InvalidOrderState(String message) {
        super(message);
    }
}
