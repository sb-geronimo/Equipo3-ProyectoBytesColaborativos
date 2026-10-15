package es.bytescolab.msdashboard.client;

import es.bytescolab.msdashboard.config.InternalFeignConfig;
import es.bytescolab.msdashboard.dto.response.client.MaintenanceOrdersPage;
import es.bytescolab.msdashboard.dto.response.client.MaintenanceStats;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "ms-maintenance", url = "${clients.maintenance-service}",
        configuration = InternalFeignConfig.class)
public interface MaintenanceClient {

    @GetMapping("/api/maintenance/stats")
    MaintenanceStats getStats(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY") String granularity
    );

    @GetMapping("/api/maintenance/orders")
    MaintenanceOrdersPage listOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "dueBefore", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    );
}
