package es.bytescolab.msroutes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "RouteCreateRequest", description = "Petición para planificar una nueva ruta")
public record RouteCreateRequest(

        @NotNull
        @Schema(description = "Identificador del vehículo asignado", example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61")
        UUID vehicleId,

        @NotNull
        @Schema(description = "Identificador del conductor asignado", example = "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01")
        UUID driverId,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Punto de origen", example = "Madrid Centro Logístico")
        String origin,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Punto de destino", example = "Valencia Puerto")
        String destination,

        @NotNull
        @Schema(description = "Inicio planificado en UTC", example = "2026-10-06T07:00:00Z")
        Instant plannedStart,

        @NotNull
        @Positive
        @Schema(description = "Duración estimada en minutos", example = "240")
        Integer estimatedDurationMin,

        @NotNull
        @Positive
        @Schema(description = "Distancia planificada en km", example = "355.0")
        BigDecimal plannedDistanceKm
) {
}
