package es.bytescolab.msroutes.exception;

public class RouteOverlapException extends RuntimeException {
    public RouteOverlapException() {
        super("Ya existe una ruta planificada o en curso que se solapa con la ventana indicada");
    }
}