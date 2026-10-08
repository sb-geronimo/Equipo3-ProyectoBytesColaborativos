package es.bytescolab.msroutes.exception;

public class DriverNotEligibleException extends RuntimeException {

    public DriverNotEligibleException(String detail) {
        super(detail == null ? "El conductor no es elegible para esta ruta" : detail);
    }
}