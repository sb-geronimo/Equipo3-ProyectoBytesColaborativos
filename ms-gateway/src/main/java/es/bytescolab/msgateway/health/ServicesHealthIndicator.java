package es.bytescolab.msgateway.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ServicesHealthIndicator implements HealthIndicator {

    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);

    private final WebClient webClient;
    private final Map<String, String> services;

    public ServicesHealthIndicator(
            @Value("${services.auth.url}") String authUrl,
            @Value("${services.vehicles.url}") String vehiclesUrl,
            @Value("${services.drivers.url}") String driversUrl,
            @Value("${services.routes.url}") String routesUrl,
            @Value("${services.maintenance.url}") String maintenanceUrl,
            @Value("${services.fuel.url}") String fuelUrl,
            @Value("${services.alerts.url}") String alertsUrl,
            @Value("${services.dashboard.url}") String dashboardUrl) {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024))
                .build();

        Map<String, String> services = new LinkedHashMap<>();
        services.put("ms-auth", authUrl);
        services.put("ms-vehicles", vehiclesUrl);
        services.put("ms-drivers", driversUrl);
        services.put("ms-routes", routesUrl);
        services.put("ms-maintenance", maintenanceUrl);
        services.put("ms-fuel", fuelUrl);
        services.put("ms-alerts", alertsUrl);
        services.put("ms-dashboard", dashboardUrl);
        this.services = services;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();

        boolean allUp = true;

        for (Map.Entry<String, String> entry : services.entrySet()) {
            boolean isUp = checkService(entry.getValue());
            details.put(entry.getKey(), isUp ? "UP" : "DOWN");
            if (!isUp)
                allUp = false;
        }

        if (allUp) {
            return Health.up().withDetails(details).build();
        }
        return Health.down().withDetails(details).build();
    }

    private boolean checkService(String url) {
        try {
            webClient.get().uri(url)
                    .retrieve()
                    .toBodilessEntity()
                    .block(HEALTH_TIMEOUT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}