package es.bytescolab.msalerts.exception;

public class DependencyUnavailableException extends AlertBusinessException {

    public DependencyUnavailableException(String serviceName) {
        super("SERVICE_UNAVAILABLE", "El servicio " + serviceName + " no está disponible");
    }
}