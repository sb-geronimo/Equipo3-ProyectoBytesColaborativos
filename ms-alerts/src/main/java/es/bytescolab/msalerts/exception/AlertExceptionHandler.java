package es.bytescolab.msalerts.exception;

import es.bytescolab.msalerts.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class AlertExceptionHandler {

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAlertNotFound(AlertNotFoundException ex) {
        log.warn("Alerta no encontrada: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(AlertRuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRuleNotFound(AlertRuleNotFoundException ex) {
        log.warn("Regla no encontrada: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidAlertStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidAlertStateException ex) {
        log.warn("Estado de alerta inválido: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(InvalidAlertRuleParamsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParams(InvalidAlertRuleParamsException ex) {
        log.warn("Parámetros inválidos para regla: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleDependencyUnavailable(DependencyUnavailableException ex) {
        log.error("Dependencia no disponible: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, AlertBusinessException ex) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .error(ex.getErrorCode())
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }
}