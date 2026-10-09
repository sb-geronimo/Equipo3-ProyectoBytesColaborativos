package es.bytescolab.msroutes.exception;

import org.springframework.http.HttpStatus;

public class InvalidRouteStateException extends BusinessException {

    public InvalidRouteStateException(String message) {
        super("INVALID_ROUTE_STATE", HttpStatus.CONFLICT, message);
    }
}
