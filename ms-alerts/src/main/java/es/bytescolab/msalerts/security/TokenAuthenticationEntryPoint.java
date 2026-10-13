package es.bytescolab.msalerts.security;

import es.bytescolab.msalerts.dto.response.ErrorResponse;
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

    public static final String TOKEN_ERROR_ATTRIBUTE = "jwt.error";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String TOKEN_BAD_SIGNATURE = "TOKEN_BAD_SIGNATURE";
    public static final String TOKEN_MALFORMED = "TOKEN_MALFORMED";
    public static final String TOKEN_MISSING = "TOKEN_MISSING";
    public static final String TOKEN_INVALID = "TOKEN_INVALID";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String reason = asString(request.getAttribute(TOKEN_ERROR_ATTRIBUTE));
        String error;
        String message;

        switch (reason) {
            case TOKEN_EXPIRED -> {
                error = TOKEN_EXPIRED;
                message = "El token ha expirado";
            }
            case TOKEN_BAD_SIGNATURE -> {
                error = TOKEN_BAD_SIGNATURE;
                message = "La firma del token no es válida";
            }
            case TOKEN_MALFORMED -> {
                error = TOKEN_MALFORMED;
                message = "El token está malformado";
            }
            case TOKEN_MISSING -> {
                error = TOKEN_MISSING;
                message = "Falta el token de autenticación";
            }
            default -> {
                error = "UNAUTHORIZED";
                message = "Token de autenticación no válido";
            }
        }

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

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }
}