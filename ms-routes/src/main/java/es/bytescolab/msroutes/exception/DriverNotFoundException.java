package es.bytescolab.msroutes.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DriverNotFoundException extends BusinessException {

    public DriverNotFoundException(UUID driverId) {
        super("DRIVER_NOT_FOUND", HttpStatus.NOT_FOUND,
                "No existe un conductor con el ID " + driverId);
    }
}
