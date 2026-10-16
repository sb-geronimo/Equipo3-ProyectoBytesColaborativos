package es.bytescolab.msfuel.service.helper;

import es.bytescolab.msfuel.dto.response.RefuelResponse;
import es.bytescolab.msfuel.enums.FuelType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefuelCsvExporterTest {

    @Test
    void exportToCsv_listaVaciaDevuelveSoloCabecera() {
        String csv = RefuelCsvExporter.exportToCsv(List.of());

        assertEquals("id,vehicleId,refueledAt,fuelType,liters,pricePerLiter,"
                + "totalCost,odometerKm,fullTank,station,consumptionL100km\r\n", csv);
    }

    @Test
    void exportToCsv_mapeaCamposEnOrdenConNulosVacios() {
        RefuelResponse refuel = new RefuelResponse(
                UUID.fromString("b7a3f9e2-4c18-4d5a-9e60-3f1d8c2a7b01"),
                UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01"),
                Instant.parse("2026-10-06T12:15:00Z"),
                FuelType.DIESEL,
                new BigDecimal("58.40"),
                new BigDecimal("1.62"),
                new BigDecimal("94.61"),
                45572,
                true,
                "Repsol A-3 km 112",
                null);

        String csv = RefuelCsvExporter.exportToCsv(List.of(refuel));

        assertTrue(csv.startsWith("id,vehicleId,refueledAt,fuelType,liters,pricePerLiter,"
                + "totalCost,odometerKm,fullTank,station,consumptionL100km\r\n"));
        assertEquals("b7a3f9e2-4c18-4d5a-9e60-3f1d8c2a7b01,"
                + "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01,"
                + "2026-10-06T12:15:00Z,DIESEL,58.40,1.62,94.61,45572,true,"
                + "Repsol A-3 km 112,\r\n",
                csv.substring(csv.indexOf("\r\n") + 2));
    }

    @Test
    void exportToCsv_escapaComasComillasYSaltos() {
        RefuelResponse refuel = new RefuelResponse(
                UUID.fromString("b7a3f9e2-4c18-4d5a-9e60-3f1d8c2a7b01"),
                UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01"),
                Instant.parse("2026-10-06T12:15:00Z"),
                FuelType.DIESEL,
                new BigDecimal("58.40"),
                new BigDecimal("1.62"),
                new BigDecimal("94.61"),
                45572,
                true,
                "Estación \"Centro\", km 5\nsegunda línea",
                new BigDecimal("11.30"));

        String csv = RefuelCsvExporter.exportToCsv(List.of(refuel));

        assertTrue(csv.contains("\"Estación \"\"Centro\"\", km 5\nsegunda línea\""));
    }
}
