package es.bytescolab.msdashboard.dto.response.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FuelConsumption(
        LocalDate from,
        LocalDate to,
        List<VehicleFuelConsumption> vehicles
) {

    public record VehicleFuelConsumption(
            String vehicleId,
            String plate,
            String type,
            BigDecimal liters,
            BigDecimal cost,
            Integer distanceKm,
            BigDecimal avgL100km
    ) {
    }
}
