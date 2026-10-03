package es.bytescolab.msauth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";
    private static final String INTERNAL_PATH = "/api/auth/validate";

    private final ErrorResponseWriter errorResponseWriter;

    @Value("${internal.api-key:}")
    private String apiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !INTERNAL_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String providedKey = request.getHeader(INTERNAL_KEY_HEADER);

        if (providedKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Se recibió {} pero internal.api-key no está configurada", INTERNAL_KEY_HEADER);
            filterChain.doFilter(request, response);
            return;
        }

        if (!apiKey.equals(providedKey)) {
            log.warn("Clave interna inválida en {}", request.getRequestURI());
            errorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Clave interna inválida");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal", null, List.of(new SimpleGrantedAuthority("MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
