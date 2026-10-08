package es.bytescolab.msroutes.exception;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException() {
        super("No existe un vehiculo con el ID proporcionado");
    }
}