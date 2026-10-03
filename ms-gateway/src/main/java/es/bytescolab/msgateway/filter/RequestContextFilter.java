package es.bytescolab.msgateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter implements WebFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest mutated = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(INTERNAL_KEY_HEADER);
            if (headers.getFirst(REQUEST_ID_HEADER) == null) {
                headers.set(REQUEST_ID_HEADER, UUID.randomUUID().toString());
            }
        }).build();

        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER,
                mutated.getHeaders().getFirst(REQUEST_ID_HEADER));

        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
