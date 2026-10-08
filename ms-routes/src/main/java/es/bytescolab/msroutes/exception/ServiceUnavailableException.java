package es.bytescolab.msroutes.exception;

public class ServiceUnavailableException extends RuntimeException {

    private final String serviceName;

    public ServiceUnavailableException(String serviceName, String detail) {
        super(buildMessage(serviceName, detail));
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    private static String buildMessage(String serviceName, String detail) {
        String safeName = serviceName == null ? "dependencia" : serviceName;
        String safeDetail = detail == null ? "" : detail;
        return "El servicio " + safeName + " no responde (" + safeDetail + ")";
    }
}