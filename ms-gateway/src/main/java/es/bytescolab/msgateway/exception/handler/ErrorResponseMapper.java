package es.bytescolab.msgateway.exception.handler;

import es.bytescolab.msgateway.exception.domain.GatewayDomainException;
import es.bytescolab.msgateway.exception.domain.ResourceNotFoundException;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class ErrorResponseMapper {

    private final Map<Class<? extends Throwable>, ErrorResponseStrategy> strategies;

    public ErrorResponseMapper() {
        this.strategies = Map.of(
                ResourceNotFoundException.class, this::mapResourceNotFound,
                NotFoundException.class, this::mapNotFound,
                ResponseStatusException.class, this::mapResponseStatus,
                ConnectException.class, this::mapConnectionError,
                TimeoutException.class, this::mapTimeoutError,
                org.springframework.cloud.gateway.support.ServiceUnavailableException.class, this::mapGatewayServiceUnavailable
        );
    }

    public ErrorContext resolve(Throwable ex) {
        ErrorResponseStrategy strategy = strategies.get(ex.getClass());
        if (strategy != null) {
            return strategy.map(ex);
        }

        // Buscar en jerarquía de clases
        for (Map.Entry<Class<? extends Throwable>, ErrorResponseStrategy> entry : strategies.entrySet()) {
            if (entry.getKey().isInstance(ex)) {
                return entry.getValue().map(ex);
            }
        }

        return mapDefault(ex);
    }

    private ErrorContext mapResourceNotFound(Throwable ex) {
        GatewayDomainException ge = (GatewayDomainException) ex;
        return ErrorContext.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(ex.getMessage())
                .errorCode(ge.getErrorCode())
                .metadata(ge.getMetadata())
                .build();
    }

    private ErrorContext mapNotFound(Throwable ex) {
        return ErrorContext.builder()
                .status(HttpStatus.NOT_FOUND)
                .message("El servicio o recurso solicitado no existe")
                .errorCode("NOT_FOUND")
                .build();
    }

    private ErrorContext mapConnectionError(Throwable ex) {
        return ErrorContext.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message("Servicio no disponible")
                .errorCode("SERVICE_UNAVAILABLE")
                .build();
    }

    private ErrorContext mapTimeoutError(Throwable ex) {
        return ErrorContext.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message("Tiempo de espera agotado")
                .errorCode("SERVICE_TIMEOUT")
                .build();
    }

    private ErrorContext mapGatewayServiceUnavailable(Throwable ex) {
        return ErrorContext.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .message("Servicio no disponible")
                .errorCode("SERVICE_UNAVAILABLE")
                .build();
    }

    private ErrorContext mapResponseStatus(Throwable ex) {
        ResponseStatusException rse = (ResponseStatusException) ex;
        HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }

        return ErrorContext.builder()
                .status(status)
                .message(rse.getReason() != null ? rse.getReason() : status.getReasonPhrase())
                .errorCode(status.name())
                .build();
    }

    private ErrorContext mapDefault(Throwable ex) {
        return ErrorContext.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Error interno del servidor")
                .errorCode("INTERNAL_ERROR")
                .build();
    }
}
