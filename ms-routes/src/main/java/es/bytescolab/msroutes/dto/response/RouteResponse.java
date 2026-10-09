package es.bytescolab.msroutes.dto.response;

import es.bytescolab.msroutes.enums.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@Schema(name = "RouteResponse", description = "Detalle de una ruta")
public record RouteResponse(

        @Schema(description = "Identificador de la ruta", example = "5b9e2c10-7f4a-4d55-b1a0-3c8e6f7a9201")
        UUID id,

        @Schema(description = "Identificador del vehículo asignado", example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61")
        UUID vehicleId,

        @Schema(description = "Identificador del conductor asignado", example = "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01")
        UUID driverId,

        @Schema(description = "Punto de origen", example = "Madrid Centro Logístico")
        String origin,

        @Schema(description = "Punto de destino", example = "Valencia Puerto")
        String destination,

        @Schema(description = "Inicio planificado en UTC", example = "2026-10-06T07:00:00Z")
        Instant plannedStart,

        @Schema(description = "Duración estimada en minutos", example = "240")
        Integer estimatedDurationMin,

        @Schema(description = "Distancia planificada en km", example = "355.0")
        BigDecimal plannedDistanceKm,

        @Schema(description = "Estado actual de la ruta", example = "PLANNED")
        RouteStatus status,

        @Schema(description = "Marca de tiempo de inicio real en UTC", example = "2026-10-06T07:05:12Z")
        Instant startedAt,

        @Schema(description = "Marca de tiempo de fin en UTC", example = "2026-10-06T12:48:30Z")
        Instant endedAt,

        @Schema(description = "Lectura del odómetro al iniciar la ruta en km", example = "145230")
        Integer startOdometerKm,

        @Schema(description = "Lectura del odómetro al finalizar la ruta en km", example = "145595")
        Integer endOdometerKm,

        @Schema(description = "Distancia real recorrida en km", example = "362.4")
        BigDecimal actualDistanceKm,

        @Schema(description = "Observaciones registradas durante la ruta")
        String notes,

        @Schema(description = "Identificador del usuario que planificó la ruta",
                example = "9a4d2e10-7c4a-4b55-b1a0-3c8e6f7a9210")
        UUID createdBy,

        @Schema(description = "Marca de tiempo de creación en UTC", example = "2026-10-05T16:20:00Z")
        Instant createdAt,

        @Schema(description = "Marca de tiempo de última actualización en UTC",
                example = "2026-10-06T12:48:30Z")
        Instant updatedAt
) {
}
