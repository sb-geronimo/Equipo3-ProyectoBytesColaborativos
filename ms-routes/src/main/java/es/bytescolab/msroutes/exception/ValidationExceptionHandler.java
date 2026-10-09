package es.bytescolab.msroutes.exception;

import es.bytescolab.msroutes.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ValidationDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.ValidationDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("Validación de body fallida: {}", details);
        return ResponseEntity.badRequest().body(
                ErrorResponse.withDetails(
                        "VALIDATION_ERROR",
                        "La petición contiene campos no válidos",
                        details
                )
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(ConstraintViolationException ex) {
        List<ErrorResponse.ValidationDetail> details = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.ValidationDetail(
                        v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        log.warn("Validación de parámetros fallida: {}", details);
        return ResponseEntity.badRequest().body(
                ErrorResponse.withDetails(
                        "VALIDATION_ERROR",
                        "Parámetros inválidos",
                        details
                )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "El parámetro '" + ex.getName() + "' tiene un formato inválido";
        log.warn("Validación de tipo fallida: {}", detail);
        return ResponseEntity.badRequest().body(
                ErrorResponse.withDetails(
                        "VALIDATION_ERROR",
                        "Parámetros inválidos",
                        List.of(new ErrorResponse.ValidationDetail(ex.getName(), detail))
                )
        );
    }
}
