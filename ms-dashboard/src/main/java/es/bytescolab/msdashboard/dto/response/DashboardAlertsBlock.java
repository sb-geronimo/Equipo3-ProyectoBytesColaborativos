package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DashboardAlertsBlock", description = "Snapshot de alertas actualmente abiertas desglosado por severidad")
public record DashboardAlertsBlock(

        @Schema(description = "Total de alertas actualmente abiertas", example = "11")
        Long open,

        @Schema(description = "Alertas abiertas con severidad CRITICAL", example = "2")
        Long critical,

        @Schema(description = "Alertas abiertas con severidad WARNING", example = "9")
        Long warning,

        @Schema(description = "Alertas abiertas con severidad INFO", example = "0")
        Long info
) {
}
