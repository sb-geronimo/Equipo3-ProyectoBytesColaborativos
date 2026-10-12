package es.bytescolab.msalerts.dto.response;

import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "AlertResponse", description = "Detalle de una alerta generada por el evaluador")
public record AlertResponse(

        @Schema(description = "Identificador único de la alerta")
        UUID id,

        @Schema(description = "Tipo de alerta", example = "MAINTENANCE_OVERDUE")
        AlertType type,

        @Schema(description = "Severidad asignada", example = "CRITICAL")
        AlertSeverity severity,

        @Schema(description = "Estado actual", example = "OPEN")
        AlertStatus status,

        @Schema(description = "Vehículo afectado, si aplica")
        UUID vehicleId,

        @Schema(description = "Conductor afectado, si aplica")
        UUID driverId,

        @Schema(description = "Mensaje legible que describe la alerta")
        String message,

        @Schema(description = "Fecha de creación de la alerta")
        Instant createdAt,

        @Schema(description = "Fecha de reconocimiento, null si no se ha reconocido")
        Instant acknowledgedAt,

        @Schema(description = "Identificador del usuario que reconoció la alerta")
        UUID acknowledgedBy,

        @Schema(description = "Comentario dejado al reconocer la alerta")
        String comment,

        @Schema(description = "Fecha de resolución, null si sigue activa")
        Instant resolvedAt
) {
}