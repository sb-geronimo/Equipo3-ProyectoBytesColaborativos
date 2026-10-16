package es.bytescolab.msroutes.service.helper;

import es.bytescolab.msroutes.dto.response.RouteResponse;
import es.bytescolab.msroutes.enums.RouteStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteCsvExporterTest {

    @Test
    void exportToCsv_listaVaciaDevuelveSoloCabecera() {
        String csv = RouteCsvExporter.exportToCsv(List.of());

        assertEquals("id,vehicleId,driverId,origin,destination,plannedStart,"
                + "estimatedDurationMin,plannedDistanceKm,status,startedAt,endedAt,"
                + "startOdometerKm,endOdometerKm,actualDistanceKm,notes,createdBy,createdAt,updatedAt\r\n",
                csv);
    }

    @Test
    void exportToCsv_mapeaCamposEnOrdenConNulosVacios() {
        RouteResponse route = new RouteResponse(
                UUID.fromString("5b9e2c10-7f4a-4d55-b1a0-3c8e6f7a9201"),
                UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61"),
                UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01"),
                "Madrid - Centro Logístico",
                "Valencia - Puerto",
                Instant.parse("2026-10-06T07:00:00Z"),
                240,
                new BigDecimal("355.0"),
                RouteStatus.COMPLETED,
                Instant.parse("2026-10-06T07:04:12Z"),
                Instant.parse("2026-10-06T11:20:45Z"),
                45210,
                45572,
                new BigDecimal("362.4"),
                null,
                UUID.fromString("9a4d2e10-7c4a-4b55-b1a0-3c8e6f7a9210"),
                Instant.parse("2026-10-05T16:20:00Z"),
                Instant.parse("2026-10-06T11:20:45Z"));

        String csv = RouteCsvExporter.exportToCsv(List.of(route));
        String row = csv.substring(csv.indexOf("\r\n") + 2);

        assertTrue(csv.startsWith("id,vehicleId,driverId,origin,destination,plannedStart,"));
        assertEquals("5b9e2c10-7f4a-4d55-b1a0-3c8e6f7a9201,"
                + "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61,"
                + "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01,"
                + "Madrid - Centro Logístico,Valencia - Puerto,"
                + "2026-10-06T07:00:00Z,240,355.0,COMPLETED,"
                + "2026-10-06T07:04:12Z,2026-10-06T11:20:45Z,"
                + "45210,45572,362.4,,"
                + "9a4d2e10-7c4a-4b55-b1a0-3c8e6f7a9210,"
                + "2026-10-05T16:20:00Z,2026-10-06T11:20:45Z\r\n",
                row);
    }

    @Test
    void exportToCsv_escapaComasComillasYSaltos() {
        RouteResponse route = new RouteResponse(
                UUID.fromString("5b9e2c10-7f4a-4d55-b1a0-3c8e6f7a9201"),
                UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a61"),
                UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01"),
                "Madrid, \"Centro\"",
                "Valencia\nPuerto",
                Instant.parse("2026-10-06T07:00:00Z"),
                240,
                new BigDecimal("355.0"),
                RouteStatus.PLANNED,
                null,
                null,
                null,
                null,
                null,
                "Sin incidencias",
                UUID.fromString("9a4d2e10-7c4a-4b55-b1a0-3c8e6f7a9210"),
                Instant.parse("2026-10-05T16:20:00Z"),
                Instant.parse("2026-10-05T16:20:00Z"));

        String csv = RouteCsvExporter.exportToCsv(List.of(route));

        assertTrue(csv.contains("\"Madrid, \"\"Centro\"\"\""));
        assertTrue(csv.contains("\"Valencia\nPuerto\""));
    }
}
