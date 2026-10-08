package es.bytescolab.msroutes.client;

import es.bytescolab.msroutes.client.dto.VehicleFeignResponse;
import es.bytescolab.msroutes.exception.ServiceUnavailableException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "ms-vehicles",
        url = "${app.ms-vehicles.url}",
        configuration = InternalFeignConfig.class
)
public interface VehicleFeignClient {

    @GetMapping("/api/vehicles/{vehicleId}")
    VehicleFeignResponse findById(@PathVariable("vehicleId") UUID vehicleId);

    default VehicleFeignResponse findByIdFallback(UUID vehicleId, Throwable cause) {
        throw new ServiceUnavailableException("ms-vehicles",
                cause == null ? "fallback invoked" : cause.getMessage());
    }
}