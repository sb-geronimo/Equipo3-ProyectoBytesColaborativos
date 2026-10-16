package es.bytescolab.msdashboard.client;

import es.bytescolab.msdashboard.config.InternalFeignConfig;
import es.bytescolab.msdashboard.dto.response.client.VehicleFleetSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ms-vehicles", url = "${clients.vehicle-service}",
        configuration = InternalFeignConfig.class)
public interface VehicleClient {

    @GetMapping("/api/vehicles/summary")
    VehicleFleetSummary getFleetSummary();
}
