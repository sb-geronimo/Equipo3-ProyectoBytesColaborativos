package es.bytescolab.msgateway.exception.domain;

public class ResourceNotFoundException extends GatewayDomainException {

    public ResourceNotFoundException(String resource) {
        super("NOT_FOUND", "Recurso no encontrado");
        withMetadata("resource", resource);
    }
}
