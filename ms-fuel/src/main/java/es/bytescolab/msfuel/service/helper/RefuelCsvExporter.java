package es.bytescolab.msfuel.service.helper;

import es.bytescolab.msfuel.dto.response.RefuelResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public final class RefuelCsvExporter {

    private static final String HEADER = "id,vehicleId,refueledAt,fuelType,liters,pricePerLiter,"
            + "totalCost,odometerKm,fullTank,station,consumptionL100km";

    private RefuelCsvExporter() {
    }

    public static String exportToCsv(List<RefuelResponse> refuels) {
        String rows = refuels.stream()
                .map(RefuelCsvExporter::toCsvRow)
                .collect(Collectors.joining("\r\n"));
        return rows.isEmpty() ? HEADER + "\r\n" : HEADER + "\r\n" + rows + "\r\n";
    }

    private static String toCsvRow(RefuelResponse refuel) {
        return String.join(",",
                escape(refuel.id()),
                escape(refuel.vehicleId()),
                escape(refuel.refueledAt()),
                escape(refuel.fuelType()),
                formatDecimal(refuel.liters()),
                formatDecimal(refuel.pricePerLiter()),
                formatDecimal(refuel.totalCost()),
                escape(refuel.odometerKm()),
                escape(refuel.fullTank()),
                escape(refuel.station()),
                formatDecimal(refuel.consumptionL100km()));
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
