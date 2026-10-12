package es.bytescolab.msfuel.exception;

import java.util.UUID;

public class VehicleNotFoundException extends FuelBusinessException {

    public VehicleNotFoundException(UUID vehicleId) {
        super("VEHICLE_NOT_FOUND", "No existe un vehículo con el ID " + vehicleId);
    }
}
