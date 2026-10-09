package es.bytescolab.msmaintenance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CompleteOrderRequest(

        @NotNull
        @PositiveOrZero
        BigDecimal cost,

        @PositiveOrZero
        Integer odometerKm,

        @NotBlank
        String workshop,

        String notes
) {
}
