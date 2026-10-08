package es.bytescolab.msroutes.exception;

public class RouteNotFoundException extends RuntimeException {
    public RouteNotFoundException() {
        super("No existe una ruta con el ID proporcionado");
    }
}