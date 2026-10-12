package es.bytescolab.msalerts.client;

import es.bytescolab.msalerts.config.InternalFeignConfig;
import es.bytescolab.msalerts.dto.response.client.DriverSummaryResponse;
import es.bytescolab.msalerts.dto.response.client.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ms-drivers", url = "${clients.driver-service}",
        configuration = InternalFeignConfig.class)
public interface DriverClient {

    @GetMapping("/api/drivers")
    PageResponse<DriverSummaryResponse> listDrivers(
            @RequestParam(value = "licenseExpiringInDays", required = false) Integer licenseExpiringInDays,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    );
}