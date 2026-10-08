package es.bytescolab.msroutes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Datos para planificar una ruta asignando un vehiculo y un conductor")
public record CreateRouteRequest(

        @Schema(description = "Identificador del vehiculo asignado",
                example = "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01")
        @NotNull(message = "El identificador del vehiculo es obligatorio")
        UUID vehicleId,

        @Schema(description = "Identificador del conductor asignado",
                example = "3b2d8f10-7c4a-4e55-bla0-5d9e2c7f1a01")
        @NotNull(message = "El identificador del conductor es obligatorio")
        UUID driverId,

        @Schema(description = "Punto de origen de la ruta", example = "Madrid - Centro Logistico")
        @NotBlank(message = "El origen es obligatorio")
        @Size(max = 255, message = "El origen no puede superar los 255 caracteres")
        String origin,

        @Schema(description = "Punto de destino de la ruta", example = "Valencia - Puerto")
        @NotBlank(message = "El destino es obligatorio")
        @Size(max = 255, message = "El destino no puede superar los 255 caracteres")
        String destination,

        @Schema(description = "Inicio planificado en UTC (ISO-8601)", example = "2026-10-06T07:00:00Z")
        @NotNull(message = "El inicio planificado es obligatorio")
        @Future(message = "El inicio planificado debe ser posterior al instante actual")
        Instant plannedStart,

        @Schema(description = "Duracion estimada en minutos (1-10080, una semana)", example = "240")
        @NotNull(message = "La duracion estimada es obligatoria")
        @Min(value = 1, message = "La duracion estimada debe ser mayor que 0")
        @Max(value = 10080, message = "La duracion estimada no puede superar los 10080 minutos")
        Integer estimatedDurationMin,

        @Schema(description = "Distancia planificada en kilometros", example = "355.00")
        @NotNull(message = "La distancia planificada es obligatoria")
        @DecimalMin(value = "0.01", message = "La distancia planificada debe ser mayor que 0")
        BigDecimal plannedDistanceKm
) {
}