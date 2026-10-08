package es.bytescolab.msroutes.dto.response;

import es.bytescolab.msroutes.enums.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Resumen de una ruta (usado en el listado paginado)")
public record RouteSummaryResponse(

        @Schema(description = "Identificador unico de la ruta",
                example = "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201")
        UUID id,

        @Schema(description = "Vehiculo asignado", example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
        UUID vehicleId,

        @Schema(description = "Conductor asignado", example = "3b2d8f10-7c4a-4e55-bla0-5d9e2c7f1a01")
        UUID driverId,

        @Schema(description = "Inicio planificado en UTC (ISO-8601)", example = "2026-10-06T07:00:00Z")
        Instant plannedStart,

        @Schema(description = "Distancia planificada en kilometros", example = "355.00")
        BigDecimal plannedDistanceKm,

        @Schema(description = "Estado de la ruta",
                allowableValues = {"PLANNED", "IN_PROGRESS", "COMPLETED"},
                example = "PLANNED")
        RouteStatus status,

        @Schema(description = "Fecha de creacion en UTC (ISO-8601)", example = "2026-10-05T10:30:00Z")
        Instant createdAt
) {
}