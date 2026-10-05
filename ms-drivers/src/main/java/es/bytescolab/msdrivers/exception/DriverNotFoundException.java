package es.bytescolab.msdrivers.exception;

public class DriverNotFoundException extends RuntimeException {
    public DriverNotFoundException() {
        super("No existe un conductor con el ID proporcionado");
    }
}
