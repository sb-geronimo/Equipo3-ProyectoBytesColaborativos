package es.bytescolab.msalerts.security;

import es.bytescolab.msalerts.dto.response.ErrorResponse;
import jakarta.annotation.PostConstruct;
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

    private String configuredKey;

    @PostConstruct
    void initialize() {
        this.configuredKey = apiKey == null ? "" : apiKey.trim();
        if (configuredKey.isBlank()) {
            log.warn("internal.api-key está vacía o no definida — las llamadas internas Feign con {} serán rechazadas con 503",
                    INTERNAL_KEY_HEADER);
        } else {
            log.info("internal.api-key configurada ({} caracteres, prefijo '{}***')",
                    configuredKey.length(), configuredKey.substring(0, Math.min(2, configuredKey.length())));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getHeader(INTERNAL_KEY_HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String providedKey = request.getHeader(INTERNAL_KEY_HEADER);

        if (configuredKey.isBlank()) {
            log.error("Recibido {} pero internal.api-key no está configurada en {}", INTERNAL_KEY_HEADER, request.getRequestURI());
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "INTERNAL_API_KEY_NOT_CONFIGURED",
                    "La clave interna no está configurada en el servicio");
            return;
        }

        if (!configuredKey.equals(providedKey)) {
            log.warn("Clave interna inválida en {}", request.getRequestURI());
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Clave interna inválida");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal", null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String error, String message) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .build();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}