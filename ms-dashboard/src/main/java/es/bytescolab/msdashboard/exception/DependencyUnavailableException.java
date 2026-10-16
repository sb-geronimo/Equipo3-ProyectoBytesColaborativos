package es.bytescolab.msdashboard.exception;

public class DependencyUnavailableException extends RuntimeException {

    public DependencyUnavailableException(String nombreServicio) {
        super("El servicio " + nombreServicio + " no está disponible");
    }
}
