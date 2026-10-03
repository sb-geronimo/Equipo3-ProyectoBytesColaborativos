package es.bytescolab.msauth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String TOKEN_EXPIRED_ATTRIBUTE = "jwt.error";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        boolean expired = TOKEN_EXPIRED.equals(request.getAttribute(TOKEN_EXPIRED_ATTRIBUTE));
        if (expired) {
            errorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, TOKEN_EXPIRED, "El token ha expirado");
        } else {
            errorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Falta el token de autenticación o no es válido");
        }
    }
}
