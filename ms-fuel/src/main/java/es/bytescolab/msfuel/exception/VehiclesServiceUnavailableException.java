package es.bytescolab.msfuel.exception;

public class VehiclesServiceUnavailableException extends FuelBusinessException {

    public VehiclesServiceUnavailableException(String message) {
        super("SERVICE_UNAVAILABLE", message);
    }
}
