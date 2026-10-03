package es.bytescolab.msfuel.security;

import es.bytescolab.msfuel.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    private final ObjectMapper objectMapper;

    @Value("${internal.api-key:}")
    private String apiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getHeader(INTERNAL_KEY_HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String providedKey = request.getHeader(INTERNAL_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Se recibió {} pero internal.api-key no está configurada", INTERNAL_KEY_HEADER);
            filterChain.doFilter(request, response);
            return;
        }

        if (!apiKey.equals(providedKey)) {
            log.warn("Clave interna inválida en {}", request.getRequestURI());
            ErrorResponse body = ErrorResponse.builder()
                    .error("UNAUTHORIZED")
                    .message("Clave interna inválida")
                    .timestamp(Instant.now())
                    .build();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal", null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
