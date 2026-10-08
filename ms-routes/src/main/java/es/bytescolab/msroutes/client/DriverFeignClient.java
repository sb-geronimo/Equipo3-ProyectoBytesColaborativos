package es.bytescolab.msroutes.client;

import es.bytescolab.msroutes.client.dto.DriverFeignResponse;
import es.bytescolab.msroutes.exception.ServiceUnavailableException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "ms-drivers",
        url = "${app.ms-drivers.url}",
        configuration = InternalFeignConfig.class
)
public interface DriverFeignClient {

    @GetMapping("/api/drivers/{driverId}")
    DriverFeignResponse findById(@PathVariable("driverId") UUID driverId);

    default DriverFeignResponse findByIdFallback(UUID driverId, Throwable cause) {
        throw new ServiceUnavailableException("ms-drivers",
                cause == null ? "fallback invoked" : cause.getMessage());
    }
}