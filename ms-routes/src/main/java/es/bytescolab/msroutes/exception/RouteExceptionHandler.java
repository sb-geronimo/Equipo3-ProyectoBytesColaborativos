package es.bytescolab.msroutes.exception;

import es.bytescolab.msroutes.dto.response.ErrorResponse;
import es.bytescolab.msroutes.dto.response.ServiceUnavailableErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Objects;

@RestControllerAdvice
public class RouteExceptionHandler {

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRouteNotFoundException(RouteNotFoundException exception) {
        ErrorResponse error = new ErrorResponse("ROUTE_NOT_FOUND", exception.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVehicleNotFoundException(VehicleNotFoundException exception) {
        ErrorResponse error = new ErrorResponse("VEHICLE_NOT_FOUND", exception.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DriverNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDriverNotFoundException(DriverNotFoundException exception) {
        ErrorResponse error = new ErrorResponse("DRIVER_NOT_FOUND", exception.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(VehicleNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleVehicleNotAvailableException(VehicleNotAvailableException exception) {
        ErrorResponse error = new ErrorResponse("VEHICLE_NOT_AVAILABLE", exception.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DriverNotEligibleException.class)
    public ResponseEntity<ErrorResponse> handleDriverNotEligibleException(DriverNotEligibleException exception) {
        ErrorResponse error = new ErrorResponse("DRIVER_NOT_ELIGIBLE", exception.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(RouteOverlapException.class)
    public ResponseEntity<ErrorResponse> handleRouteOverlapException(RouteOverlapException exception) {
        ErrorResponse error = new ErrorResponse("ROUTE_OVERLAP", exception.getMessage(), Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ServiceUnavailableErrorResponse> handleServiceUnavailableException(ServiceUnavailableException exception) {
        ServiceUnavailableErrorResponse error = new ServiceUnavailableErrorResponse(
                "SERVICE_UNAVAILABLE",
                exception.getMessage(),
                exception.getServiceName(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = Objects.requireNonNull(exception.getBindingResult().getFieldError()).getDefaultMessage();
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message, Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("Error desconocido de validacion");
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message, Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}