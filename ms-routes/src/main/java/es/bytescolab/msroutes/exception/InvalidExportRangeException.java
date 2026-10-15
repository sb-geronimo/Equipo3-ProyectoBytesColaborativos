package es.bytescolab.msroutes.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;

public class InvalidExportRangeException extends BusinessException {

    public InvalidExportRangeException(LocalDate from, LocalDate to) {
        super("VALIDATION_ERROR", HttpStatus.BAD_REQUEST,
                "El parámetro 'from' (" + from + ") no puede ser posterior al parámetro 'to' (" + to + ")");
    }
}
