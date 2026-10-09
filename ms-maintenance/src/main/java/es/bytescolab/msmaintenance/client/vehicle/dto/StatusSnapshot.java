package es.bytescolab.msmaintenance.client.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.bytescolab.msmaintenance.client.vehicle.enums.VehicleStatus;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatusSnapshot(
        UUID id,
        Integer odometerKm,
        VehicleStatus status
) {
}
