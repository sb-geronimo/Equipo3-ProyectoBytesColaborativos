package es.bytescolab.msmaintenance.exception;

public class VehicleNotAvailable extends RuntimeException {
    public VehicleNotAvailable(String message) {
        super(message);
    }
}
