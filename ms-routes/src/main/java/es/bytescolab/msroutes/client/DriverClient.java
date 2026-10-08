package es.bytescolab.msroutes.client;

import es.bytescolab.msroutes.dto.response.DriverDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "driver-service", url = "${clients.driver-service}")
public interface DriverClient {

    @GetMapping("/api/drivers/{driverId}")
    DriverDetailResponse getDriver(@PathVariable UUID driverId);
}
