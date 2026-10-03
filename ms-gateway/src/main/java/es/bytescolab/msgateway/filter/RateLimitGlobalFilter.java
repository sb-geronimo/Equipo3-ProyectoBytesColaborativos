package es.bytescolab.msgateway.filter;

import tools.jackson.databind.ObjectMapper;
import es.bytescolab.msgateway.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Rate limit global por IP: un único contador por cliente, compartido por todas las rutas.
 */
@Slf4j
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final String GLOBAL_KEY = "global";
    private static final int RETRY_AFTER_SECONDS = 30;

    private final RedisRateLimiter redisRateLimiter;
    private final ObjectMapper objectMapper;
    private final int perMinute;

    public RateLimitGlobalFilter(RedisRateLimiter redisRateLimiter, ObjectMapper objectMapper,
                                 @Value("${gateway.rate-limit.per-minute:60}") int perMinute) {
        this.redisRateLimiter = redisRateLimiter;
        this.objectMapper = objectMapper;
        this.perMinute = perMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = Objects.requireNonNull(
                exchange.getRequest().getRemoteAddress()
        ).getAddress().getHostAddress();

        return redisRateLimiter.isAllowed(GLOBAL_KEY, ip)
                .flatMap(response -> {
                    if (response.isAllowed()) {
                        return chain.filter(exchange);
                    }
                    log.warn("Rate limit superado para IP: {}", ip);
                    return writeRateLimitResponse(exchange.getResponse());
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
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
}
