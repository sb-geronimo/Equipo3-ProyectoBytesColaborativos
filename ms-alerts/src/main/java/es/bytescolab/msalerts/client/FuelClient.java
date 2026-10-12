package es.bytescolab.msalerts.client;

import es.bytescolab.msalerts.config.InternalFeignConfig;
import es.bytescolab.msalerts.dto.response.client.ConsumptionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "ms-fuel", url = "${clients.fuel-service}",
        configuration = InternalFeignConfig.class)
public interface FuelClient {

    @GetMapping("/api/fuel/consumption")
    ConsumptionResponse getConsumption(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    );
}