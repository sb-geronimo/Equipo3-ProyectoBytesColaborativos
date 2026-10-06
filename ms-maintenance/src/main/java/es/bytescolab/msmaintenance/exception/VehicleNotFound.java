package es.bytescolab.msmaintenance.exception;

public class VehicleNotFound extends RuntimeException {
    public VehicleNotFound(String message) {
        super(message);
    }
}
