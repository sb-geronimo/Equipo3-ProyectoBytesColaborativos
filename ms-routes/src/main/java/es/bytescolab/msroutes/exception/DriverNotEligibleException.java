package es.bytescolab.msroutes.exception;

import org.springframework.http.HttpStatus;

public class DriverNotEligibleException extends BusinessException {

    public DriverNotEligibleException(String message) {
        super("DRIVER_NOT_ELIGIBLE", HttpStatus.CONFLICT, message);
    }
}
