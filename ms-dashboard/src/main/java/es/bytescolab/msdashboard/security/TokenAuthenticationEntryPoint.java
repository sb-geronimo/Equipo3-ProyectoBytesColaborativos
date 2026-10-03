package es.bytescolab.msdashboard.security;

import es.bytescolab.msdashboard.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String TOKEN_EXPIRED_ATTRIBUTE = "jwt.error";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        boolean expired = TOKEN_EXPIRED.equals(request.getAttribute(TOKEN_EXPIRED_ATTRIBUTE));
        String error = expired ? TOKEN_EXPIRED : "UNAUTHORIZED";
        String message = expired ? "El token ha expirado" : "Falta el token de autenticación o no es válido";

        log.warn("No autorizado ({}): {}", error, request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
