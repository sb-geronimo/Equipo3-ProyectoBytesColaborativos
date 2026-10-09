package es.bytescolab.msmaintenance.client;

import es.bytescolab.msmaintenance.client.vehicle.dto.StatusChangeRequest;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusSnapshot;
import es.bytescolab.msmaintenance.config.InternalFeignConfig;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "ms-vehicles", url = "${vehicles.service.url}",
        configuration = InternalFeignConfig.class)
public interface VehicleClient {
    @GetMapping("/api/vehicles/{vehicleId}")
    StatusSnapshot getVehicle(@PathVariable UUID vehicleId);

    @PatchMapping("/api/vehicles/{vehicleId}/status")
    void updateVehicle(
            @PathVariable UUID vehicleId, @Valid @RequestBody StatusChangeRequest request
    );
}