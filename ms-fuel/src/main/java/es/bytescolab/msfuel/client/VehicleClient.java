package es.bytescolab.msfuel.client;

import es.bytescolab.msfuel.config.InternalFeignConfig;
import es.bytescolab.msfuel.dto.response.VehicleDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "vehicle-service", url = "${clients.vehicle-service}",
        configuration = InternalFeignConfig.class)
public interface VehicleClient {

    @GetMapping("/api/vehicles/{vehicleId}")
    VehicleDetailResponse getVehicle(@PathVariable UUID vehicleId);
}
