package es.bytescolab.msauth.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, HttpStatus status, String error, String message) throws IOException {
        Map<String, Object> body = Map.of(
                "error", error,
                "message", message,
                "timestamp", Instant.now().toString()
        );
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IOException("No se pudo serializar la respuesta de error", e);
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
