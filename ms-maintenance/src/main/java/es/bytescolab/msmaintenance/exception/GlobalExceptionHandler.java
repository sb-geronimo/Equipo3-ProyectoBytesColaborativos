package es.bytescolab.msmaintenance.exception;

import es.bytescolab.msmaintenance.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(PlanAreadyExists.class)
    public ResponseEntity<ErrorResponse> handlePlanAlreadyExists(PlanAreadyExists ex) {
        return build(HttpStatus.CONFLICT, "PLAN_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ErrorResponse.builder()
                        .error("SERVICE_UNAVAILABLE")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .service("ms-vehicles")
                        .build()
        );
    }

    @ExceptionHandler(VehicleNotFound.class)
    public ResponseEntity<ErrorResponse> handleVehicleNotFound(VehicleNotFound ex) {
        return build(HttpStatus.NOT_FOUND, "VEHICLE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderState.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderState(InvalidOrderState ex) {
        return build(HttpStatus.CONFLICT, "INVALID_ORDER_STATE", ex.getMessage());
    }

    @ExceptionHandler(VehicleNotAvailable.class)
    public ResponseEntity<ErrorResponse> handleVehicleNotAvailable(VehicleNotAvailable ex) {
        return build(HttpStatus.CONFLICT, "VEHICLE_NOT_AVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(InvalidOdometerException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOdometer(InvalidOdometerException ex) {
        return build(HttpStatus.CONFLICT, "INVALID_ODOMETER", ex.getMessage());
    }

    @ExceptionHandler(OrderNotFound.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFound ex) {
        return build(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.Detail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.Detail(e.getField(), e.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La petición contiene campos no válidos", details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "Error interno del servidor");
    }


    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return build(status, error, message, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message,
                                                List<ErrorResponse.Detail> details) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .error(error)
                        .message(message)
                        .details(details)
                        .timestamp(Instant.now())
                        .service(null)
                        .build()
        );
    }
}
