package es.bytescolab.msfuel.exception;

import java.time.LocalDate;

public class InvalidExportRangeException extends FuelBusinessException {

    public InvalidExportRangeException(LocalDate from, LocalDate to) {
        super("VALIDATION_ERROR",
                "El parámetro 'from' (" + from + ") no puede ser posterior al parámetro 'to' (" + to + ")");
    }
}
