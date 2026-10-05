package es.bytescolab.msdrivers.exception;

public class LicenseNumberAlreadyExistsException extends RuntimeException {
    public LicenseNumberAlreadyExistsException() {
        super("El numero de licencia proporcionado ya se encuentra registrado en el sistema.");
    }
}
