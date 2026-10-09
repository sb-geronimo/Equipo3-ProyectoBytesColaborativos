package es.bytescolab.msroutes.exception;

import org.springframework.http.HttpStatus;

public class VehicleNotAvailableException extends BusinessException {

    public VehicleNotAvailableException(String message) {
        super("VEHICLE_NOT_AVAILABLE", HttpStatus.CONFLICT, message);
    }
}
