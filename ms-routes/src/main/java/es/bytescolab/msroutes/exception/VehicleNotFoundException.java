package es.bytescolab.msroutes.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class VehicleNotFoundException extends BusinessException {

    public VehicleNotFoundException(UUID vehicleId) {
        super("VEHICLE_NOT_FOUND", HttpStatus.NOT_FOUND,
                "No existe un vehículo con el ID " + vehicleId);
    }
}
