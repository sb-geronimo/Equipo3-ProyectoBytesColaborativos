package es.bytescolab.msroutes.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class RouteNotFoundException extends BusinessException {

    public RouteNotFoundException(UUID routeId) {
        super("ROUTE_NOT_FOUND", HttpStatus.NOT_FOUND,
                "No existe una ruta con el ID " + routeId);
    }
}
