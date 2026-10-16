package es.bytescolab.msroutes.service.helper;

import es.bytescolab.msroutes.dto.response.RouteResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public final class RouteCsvExporter {

    private static final String HEADER = "id,vehicleId,driverId,origin,destination,plannedStart,"
            + "estimatedDurationMin,plannedDistanceKm,status,startedAt,endedAt,"
            + "startOdometerKm,endOdometerKm,actualDistanceKm,notes,createdBy,createdAt,updatedAt";

    private RouteCsvExporter() {
    }

    public static String exportToCsv(List<RouteResponse> routes) {
        String rows = routes.stream()
                .map(RouteCsvExporter::toCsvRow)
                .collect(Collectors.joining("\r\n"));
        return rows.isEmpty() ? HEADER + "\r\n" : HEADER + "\r\n" + rows + "\r\n";
    }

    private static String toCsvRow(RouteResponse route) {
        return String.join(",",
                escape(route.id()),
                escape(route.vehicleId()),
                escape(route.driverId()),
                escape(route.origin()),
                escape(route.destination()),
                escape(route.plannedStart()),
                escape(route.estimatedDurationMin()),
                formatDecimal(route.plannedDistanceKm()),
                escape(route.status()),
                escape(route.startedAt()),
                escape(route.endedAt()),
                escape(route.startOdometerKm()),
                escape(route.endOdometerKm()),
                formatDecimal(route.actualDistanceKm()),
                escape(route.notes()),
                escape(route.createdBy()),
                escape(route.createdAt()),
                escape(route.updatedAt()));
    }

    private static String formatDecimal(BigDecimal value) {
        return value == null ? "" : escape(value.toPlainString());
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
