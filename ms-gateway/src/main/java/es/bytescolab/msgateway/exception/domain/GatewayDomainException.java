package es.bytescolab.msgateway.exception.domain;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class GatewayDomainException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> metadata;

    protected GatewayDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
    }

    public GatewayDomainException withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
}
