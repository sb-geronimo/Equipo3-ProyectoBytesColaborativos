package es.bytescolab.msalerts.client;

import es.bytescolab.msalerts.config.InternalFeignConfig;
import es.bytescolab.msalerts.dto.response.client.VehicleDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "ms-vehicles", url = "${clients.vehicle-service}",
        configuration = InternalFeignConfig.class)
public interface VehicleClient {

    @GetMapping("/api/vehicles/{vehicleId}")
    VehicleDetailResponse getVehicle(@PathVariable UUID vehicleId);
}