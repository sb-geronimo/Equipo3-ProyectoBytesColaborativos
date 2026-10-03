package es.bytescolab.msgateway.controller;

import es.bytescolab.msgateway.dto.HealthResponseDto;
import es.bytescolab.msgateway.health.ServicesHealthIndicator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Gateway", description = "Estado del gateway y de los servicios")
public class HealthController {

    private static final String GATEWAY_STATUS = "UP";

    private final ServicesHealthIndicator servicesHealthIndicator;

    @Operation(
            summary = "Estado de los servicios",
            description = "Devuelve el estado de los 8 servicios y marca como DOWN los que no responden.")
    @ApiResponse(responseCode = "200", description = "Estado del gateway y de cada servicio",
            content = @Content(schema = @Schema(implementation = HealthResponseDto.class),
                    examples = @ExampleObject(name = "ms-fuel caído", value = """
                            {
                              "gateway": "UP",
                              "timestamp": "2026-10-02T10:15:30Z",
                              "services": {
                                "ms-auth": "UP",
                                "ms-vehicles": "UP",
                                "ms-drivers": "UP",
                                "ms-routes": "UP",
                                "ms-maintenance": "UP",
                                "ms-fuel": "DOWN",
                                "ms-alerts": "UP",
                                "ms-dashboard": "UP"
                              }
                            }
                            """)))
    @GetMapping("/health")
    public Mono<ResponseEntity<HealthResponseDto>> health() {
        return Mono.fromCallable(servicesHealthIndicator::health)
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::buildResponse);
    }

    private ResponseEntity<HealthResponseDto> buildResponse(Health health) {
        Map<String, String> services = extractServices(health);
        HealthResponseDto body = new HealthResponseDto(
                GATEWAY_STATUS,
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                services
        );
        return ResponseEntity.ok(body);
    }

    private Map<String, String> extractServices(Health health) {
        return health.getDetails().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.valueOf(entry.getValue())
                ));
    }
}
