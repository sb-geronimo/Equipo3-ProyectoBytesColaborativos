package es.bytescolab.msroutes.exception;

import org.springframework.http.HttpStatus;

public class RouteServiceUnavailableException extends BusinessException {

    public RouteServiceUnavailableException(String service, String message) {
        super("SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message, service);
    }
}
