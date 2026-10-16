package es.bytescolab.msdashboard.exception;

import es.bytescolab.msdashboard.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class DashboardExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String motivo = "El parámetro '" + ex.getParameterName() + "' es obligatorio (tipo "
                + ex.getParameterType() + ")";
        List<ErrorResponse.Detail> detalles = List.of(
                new ErrorResponse.Detail(ex.getParameterName(), motivo));
        log.warn("Parámetro obligatorio ausente: {}", ex.getParameterName());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La petición contiene parámetros obligatorios ausentes", detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(ConstraintViolationException ex) {
        List<ErrorResponse.Detail> detalles = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.Detail(extraerNombreParametro(v.getPropertyPath().toString()),
                        v.getMessage()))
                .toList();
        log.warn("Validación de parámetros fallida: {}", detalles);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La petición contiene parámetros no válidos", detalles);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.Detail> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.Detail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("Validación de body fallida: {}", detalles);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La petición contiene campos no válidos", detalles);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String tipoEsperado = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "valor válido";
        String motivo = "El parámetro '" + ex.getName() + "' no tiene un formato válido ("
                + tipoEsperado + " esperado)";
        List<ErrorResponse.Detail> detalles = List.of(
                new ErrorResponse.Detail(ex.getName(), motivo));
        log.warn("Parámetro con formato inválido: {}", ex.getName());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La petición contiene parámetros con formato inválido", detalles);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validación fallida: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), null);
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleDependencyUnavailable(DependencyUnavailableException ex) {
        log.error("Dependencia no disponible: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "Error interno del servidor", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String mensaje) {
        return build(status, error, mensaje, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String mensaje,
                                                List<ErrorResponse.Detail> detalles) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .error(error)
                        .message(mensaje)
                        .details(detalles)
                        .timestamp(Instant.now())
                        .build()
        );
    }

    private String extraerNombreParametro(String propertyPath) {
        int indice = propertyPath.lastIndexOf('.');
        return indice >= 0 ? propertyPath.substring(indice + 1) : propertyPath;
    }
}
