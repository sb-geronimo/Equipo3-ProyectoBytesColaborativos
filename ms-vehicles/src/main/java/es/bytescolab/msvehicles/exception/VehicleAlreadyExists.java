package es.bytescolab.msvehicles.exception;

public class VehicleAlreadyExists extends RuntimeException {
    public VehicleAlreadyExists(String message) {
        super(message);
    }
}
