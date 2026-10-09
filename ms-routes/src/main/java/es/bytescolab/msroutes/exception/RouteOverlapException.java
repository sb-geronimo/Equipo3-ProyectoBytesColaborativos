package es.bytescolab.msroutes.exception;

import org.springframework.http.HttpStatus;

public class RouteOverlapException extends BusinessException {

    public RouteOverlapException(String message) {
        super("ROUTE_OVERLAP", HttpStatus.CONFLICT, message);
    }
}
