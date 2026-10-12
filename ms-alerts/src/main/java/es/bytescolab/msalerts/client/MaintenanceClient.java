package es.bytescolab.msalerts.client;

import es.bytescolab.msalerts.config.InternalFeignConfig;
import es.bytescolab.msalerts.dto.response.client.OrderSummaryResponse;
import es.bytescolab.msalerts.dto.response.client.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ms-maintenance", url = "${clients.maintenance-service}",
        configuration = InternalFeignConfig.class)
public interface MaintenanceClient {

    @GetMapping("/api/maintenance/orders")
    PageResponse<OrderSummaryResponse> listOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    );
}