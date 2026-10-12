package es.bytescolab.msfuel.exception;

import es.bytescolab.msfuel.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        log.warn("Validación de body fallida: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .error("VALIDATION_ERROR")
                        .message("La petición contiene campos no válidos")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(ConstraintViolationException ex) {
        log.warn("Validación de parámetros fallida: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .error("VALIDATION_ERROR")
                        .message("Parámetros inválidos")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Tipo de parámetro inválido: nombre={}, valor={}", ex.getName(), ex.getValue());
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .error("VALIDATION_ERROR")
                        .message("El parámetro '" + ex.getName() + "' tiene un formato inválido")
                        .timestamp(Instant.now())
                        .build()
        );
    }
}
