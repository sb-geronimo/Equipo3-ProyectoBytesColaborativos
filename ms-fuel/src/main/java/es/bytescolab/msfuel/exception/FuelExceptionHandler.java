package es.bytescolab.msfuel.exception;

import es.bytescolab.msfuel.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class FuelExceptionHandler {

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVehicleNotFound(VehicleNotFoundException ex) {
        log.warn("Vehículo no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(InvalidOdometerException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOdometer(InvalidOdometerException ex) {
        log.warn("Odómetro inválido: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(LitersExceedTankCapacityException.class)
    public ResponseEntity<ErrorResponse> handleLitersExceedTankCapacity(
            LitersExceedTankCapacityException ex) {
        log.warn("Litros repostados exceden la capacidad del depósito: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(VehiclesServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleVehiclesServiceUnavailable(
            VehiclesServiceUnavailableException ex) {
        log.error("ms-vehicles no disponible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ErrorResponse.builder()
                        .error(ex.getErrorCode())
                        .message(ex.getMessage())
                        .service("ms-vehicles")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, FuelBusinessException ex) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .error(ex.getErrorCode())
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build()
        );
    }
}
