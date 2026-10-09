package es.bytescolab.msroutes.client;

import es.bytescolab.msroutes.config.InternalFeignConfig;
import es.bytescolab.msroutes.dto.request.VehicleStatusUpdateRequest;
import es.bytescolab.msroutes.dto.response.VehicleDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "vehicle-service", url = "${clients.vehicle-service}", configuration = InternalFeignConfig.class)
public interface VehicleClient {

    @GetMapping("/api/vehicles/{vehicleId}")
    VehicleDetailResponse getVehicle(@PathVariable UUID vehicleId);

    @PatchMapping("/api/vehicles/{vehicleId}/status")
    VehicleDetailResponse updateStatus(@PathVariable UUID vehicleId,
                                       @RequestBody VehicleStatusUpdateRequest request);
}
