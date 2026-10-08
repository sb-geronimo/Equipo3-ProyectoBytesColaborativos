package es.bytescolab.msroutes.exception;

public class VehicleNotAvailableException extends RuntimeException {
    public VehicleNotAvailableException() {
        super("El vehiculo no esta disponible para asignar a una ruta");
    }
}