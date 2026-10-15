package es.bytescolab.msdashboard.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private static final int VENTANA = 10;
    private static final float UMBRAL_FALLO = 50.0f;
    private static final Duration ESPERA_ESTADO_ABIERTO = Duration.ofSeconds(30);
    private static final int LLAMADAS_MEDIO_ABIERTO = 3;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig configuracion = CircuitBreakerConfig.custom()
                .slidingWindowSize(VENTANA)
                .failureRateThreshold(UMBRAL_FALLO)
                .waitDurationInOpenState(ESPERA_ESTADO_ABIERTO)
                .permittedNumberOfCallsInHalfOpenState(LLAMADAS_MEDIO_ABIERTO)
                .build();
        return CircuitBreakerRegistry.of(configuracion);
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig configuracion = TimeLimiterConfig.custom()
                .timeoutDuration(TIMEOUT)
                .build();
        return TimeLimiterRegistry.of(configuracion);
    }
}
