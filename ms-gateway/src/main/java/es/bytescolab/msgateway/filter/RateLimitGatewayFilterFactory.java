package es.bytescolab.msgateway.filter;

import tools.jackson.databind.ObjectMapper;
import es.bytescolab.msgateway.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@Component
public class RateLimitGatewayFilterFactory
        extends AbstractGatewayFilterFactory<RateLimitGatewayFilterFactory.Config> {

    private static final int RETRY_AFTER_SECONDS = 30;

    private final RedisRateLimiter redisRateLimiter;
    private final ObjectMapper objectMapper;
    private final int perMinute;

    public RateLimitGatewayFilterFactory(RedisRateLimiter redisRateLimiter, ObjectMapper objectMapper,
                                         @Value("${gateway.rate-limit.per-minute:60}") int perMinute) {
        super(Config.class); // <-- necesario para que Spring deserialice Config correctamente
        this.redisRateLimiter = redisRateLimiter;
        this.objectMapper = objectMapper;
        this.perMinute = perMinute;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String ip = Objects.requireNonNull(
                    exchange.getRequest().getRemoteAddress()
            ).getAddress().getHostAddress();

            return redisRateLimiter.isAllowed(config.getRouteId(), ip)
                    .flatMap(response -> {
                        if (response.isAllowed()) {
                            return chain.filter(exchange);
                        }
                        log.warn("Rate limit superado para IP: {}", ip);
                        return writeRateLimitResponse(exchange.getResponse());
                    });
        };
    }

    private Mono<Void> writeRateLimitResponse(ServerHttpResponse response) {
        ErrorResponseDto body = new ErrorResponseDto(
                "RATE_LIMIT_EXCEEDED",
                "Demasiadas peticiones. Límite: " + perMinute + " req/min",
                null,
                null,
                RETRY_AFTER_SECONDS,
                Instant.now()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);

            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().setContentLength(bytes.length);
            response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS));

            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error serializando respuesta de rate limit", e);
            return response.setComplete();
        }
    }

    public static class Config {
        private String routeId;

        public String getRouteId() {
            return routeId;
        }

        public void setRouteId(String routeId) {
            this.routeId = routeId;
        }
    }
}