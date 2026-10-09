package es.bytescolab.msroutes.exception;

import es.bytescolab.msroutes.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RouteExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("Error de negocio [{}] {}: {}", ex.getStatus().value(), ex.getErrorCode(), ex.getMessage());
        ErrorResponse body = ex.getService() != null
                ? ErrorResponse.withService(ex.getErrorCode(), ex.getMessage(), ex.getService())
                : ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
