package es.bytescolab.msgateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    private final int perMinute;

    public RateLimiterConfig(@Value("${gateway.rate-limit.per-minute:60}") int perMinute) {
        this.perMinute = perMinute;
    }

    /**
     * replenishRate = perMinute / 60 → tokens añadidos por segundo al bucket <br>
     * burstCapacity = perMinute → máximo de tokens acumulables (ráfaga de hasta N req) <br>
     * requestedTokens = 1 → cada request consume 1 token <br>
     * Con el valor por defecto (60/min) el comportamiento es 1 token/s y ráfaga de 60.
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        int replenishRate = Math.max(1, perMinute / 60);
        return new RedisRateLimiter(replenishRate, perMinute, 1);
    }
}
