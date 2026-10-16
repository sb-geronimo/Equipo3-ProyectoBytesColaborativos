package es.bytescolab.msdashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "DashboardResponse", description = "Resumen ejecutivo de la flota en una ventana temporal")
public record DashboardResponse(

        @Schema(description = "Marca de tiempo de generación en UTC",
                example = "2026-10-05T10:30:00Z")
        Instant generatedAt,

        @Schema(description = "Periodo cubierto por el resumen")
        DashboardPeriodBlock period,

        @Schema(description = "Indica si parte del resumen se construyó con datos degradados")
        boolean partial,

        @Schema(description = "Servicios origen que no respondieron",
                example = "[\"ms-fuel\"]")
        List<String> unavailable,

        @Schema(description = "Bloque agregado de inventario de la flota")
        DashboardFleetBlock fleet,

        @Schema(description = "KPIs operativos del periodo")
        DashboardKpisBlock kpis,

        @Schema(description = "Snapshot de alertas actualmente abiertas")
        DashboardAlertsBlock alerts,

        @Schema(description = "Órdenes de mantenimiento programadas próximas (estado PENDING)")
        List<DashboardUpcomingMaintenanceItem> upcomingMaintenance
) {
}
