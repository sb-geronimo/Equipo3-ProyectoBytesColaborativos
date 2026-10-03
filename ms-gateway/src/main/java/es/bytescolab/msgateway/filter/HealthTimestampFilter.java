package es.bytescolab.msgateway.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class HealthTimestampFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getURI().getPath().equals("/health")) {
            exchange.getResponse().getHeaders().add("X-Health-Timestamp",
                    Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        }
        return chain.filter(exchange);
    }
}