package es.bytescolab.msfuel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(name = "RefuelConsumptionRequest", description = "Filtros para el consumo agregado por vehículo")
public record RefuelConsumptionRequest(

        @NotNull
        @Schema(description = "Fecha inicial del rango (inclusiva)", example = "2026-09-01")
        LocalDate from,

        @NotNull
        @Schema(description = "Fecha final del rango (inclusiva)", example = "2026-09-30")
        LocalDate to
) {
}
