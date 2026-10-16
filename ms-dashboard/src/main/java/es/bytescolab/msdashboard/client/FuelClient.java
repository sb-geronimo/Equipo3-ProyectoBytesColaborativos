package es.bytescolab.msdashboard.client;

import es.bytescolab.msdashboard.config.InternalFeignConfig;
import es.bytescolab.msdashboard.dto.response.client.FuelConsumption;
import es.bytescolab.msdashboard.dto.response.client.FuelStats;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "ms-fuel", url = "${clients.fuel-service}",
        configuration = InternalFeignConfig.class)
public interface FuelClient {

    @GetMapping("/api/fuel/stats")
    FuelStats getStats(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "granularity", required = false, defaultValue = "DAY") String granularity
    );

    @GetMapping("/api/fuel/consumption")
    FuelConsumption getConsumption(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    );
}
