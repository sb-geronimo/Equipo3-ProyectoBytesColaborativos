package es.bytescolab.msdashboard.service.impl;

import es.bytescolab.msdashboard.client.AlertsClient;
import es.bytescolab.msdashboard.client.FuelClient;
import es.bytescolab.msdashboard.client.MaintenanceClient;
import es.bytescolab.msdashboard.client.RoutesClient;
import es.bytescolab.msdashboard.client.VehicleClient;
import es.bytescolab.msdashboard.dto.response.DashboardResponse;
import es.bytescolab.msdashboard.dto.response.client.AlertStats;
import es.bytescolab.msdashboard.dto.response.client.FuelStats;
import es.bytescolab.msdashboard.dto.response.client.MaintenanceOrdersPage;
import es.bytescolab.msdashboard.dto.response.client.MaintenanceStats;
import es.bytescolab.msdashboard.dto.response.client.RouteStats;
import es.bytescolab.msdashboard.dto.response.client.VehicleFleetSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAggregatorImplTest {

    @Mock
    private VehicleClient vehicleClient;

    @Mock
    private RoutesClient routesClient;

    @Mock
    private FuelClient fuelClient;

    @Mock
    private MaintenanceClient maintenanceClient;

    @Mock
    private AlertsClient alertsClient;

    private DashboardAggregatorImpl service;

    @BeforeEach
    void preparar() {
        service = new DashboardAggregatorImpl(
                vehicleClient, routesClient, fuelClient, maintenanceClient, alertsClient);
    }

    @Test
    void todosResponden_devuelveResumenCompletoSinUnavailable() {
        when(vehicleClient.getFleetSummary()).thenReturn(flota());
        when(routesClient.getStats(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(rutas());
        when(fuelClient.getStats(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(combustible());
        when(maintenanceClient.getStats(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(mantenimiento());
        when(maintenanceClient.listOrders(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(ordenesVacias());
        when(alertsClient.getStats(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(alertas());

        DashboardResponse respuesta = service.getSummary(30);

        assertThat(respuesta.partial()).isFalse();
        assertThat(respuesta.unavailable()).isEmpty();
        assertThat(respuesta.fleet()).isNotNull();
        assertThat(respuesta.fleet().total()).isEqualTo(25L);
        assertThat(respuesta.kpis().fuelLiters()).isEqualByComparingTo("4226.30");
        assertThat(respuesta.kpis().routesCompleted()).isEqualTo(412L);
        assertThat(respuesta.alerts().open()).isEqualTo(11L);
        assertThat(respuesta.alerts().critical()).isEqualTo(2L);
    }

    @Test
    void fuelNoResponde_marcaUnavailableYKpisEnNull() {
        when(vehicleClient.getFleetSummary()).thenReturn(flota());
        when(routesClient.getStats(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(rutas());
        when(fuelClient.getStats(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenThrow(new RuntimeException("ms-fuel caído"));
        when(maintenanceClient.getStats(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(mantenimiento());
        when(maintenanceClient.listOrders(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(ordenesVacias());
        when(alertsClient.getStats(any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(alertas());

        DashboardResponse respuesta = service.getSummary(30);

        assertThat(respuesta.partial()).isTrue();
        assertThat(respuesta.unavailable()).containsExactly("ms-fuel");
        assertThat(respuesta.kpis().fuelLiters()).isNull();
        assertThat(respuesta.kpis().fuelCost()).isNull();
        assertThat(respuesta.kpis().avgConsumptionL100km()).isNull();
        assertThat(respuesta.kpis().routesCompleted()).isEqualTo(412L);
        assertThat(respuesta.kpis().maintenanceCost()).isEqualByComparingTo("2140.25");
    }

    @Test
    void vehiclesNoResponde_marcaUnavailableYFlotaEnNull() {
        when(vehicleClient.getFleetSummary()).thenThrow(new RuntimeException("ms-vehicles caído"));

        DashboardResponse respuesta = service.getSummary(30);

        assertThat(respuesta.partial()).isTrue();
        assertThat(respuesta.unavailable()).contains("ms-vehicles");
        assertThat(respuesta.fleet()).isNull();
    }

    private static VehicleFleetSummary flota() {
        Map<String, Long> porEstado = new HashMap<>();
        porEstado.put("AVAILABLE", 14L);
        porEstado.put("IN_USE", 8L);
        porEstado.put("IN_MAINTENANCE", 2L);
        porEstado.put("OUT_OF_SERVICE", 1L);
        Map<String, Long> porTipo = new HashMap<>();
        porTipo.put("CAR", 9L);
        porTipo.put("VAN", 10L);
        porTipo.put("TRUCK", 4L);
        porTipo.put("MOTORCYCLE", 2L);
        return new VehicleFleetSummary(25L, porEstado, porTipo);
    }

    private static RouteStats rutas() {
        RouteStats.RouteStatsTotals totales = new RouteStats.RouteStatsTotals(
                412L, new BigDecimal("38420.50"));
        return new RouteStats(LocalDate.now(), LocalDate.now(), "DAY", totales, List.of());
    }

    private static FuelStats combustible() {
        FuelStats.FuelStatsTotals totales = new FuelStats.FuelStatsTotals(
                new BigDecimal("4226.30"),
                new BigDecimal("6846.61"),
                new BigDecimal("1.62"),
                new BigDecimal("11.00"));
        return new FuelStats(LocalDate.now(), LocalDate.now(), "DAY", totales, List.of());
    }

    private static MaintenanceStats mantenimiento() {
        MaintenanceStats.MaintenanceStatsTotals totales = new MaintenanceStats.MaintenanceStatsTotals(
                9L, new BigDecimal("2140.25"));
        return new MaintenanceStats(LocalDate.now(), LocalDate.now(), "DAY", totales,
                List.of(), List.of(), new MaintenanceStats.MaintenanceStatsUpcoming(0L, 0L));
    }

    private static MaintenanceOrdersPage ordenesVacias() {
        return new MaintenanceOrdersPage(List.of(), 0, 100, 0L, 0);
    }

    private static AlertStats alertas() {
        Map<String, Long> bySev = new HashMap<>();
        bySev.put("CRITICAL", 2L);
        bySev.put("WARNING", 9L);
        bySev.put("INFO", 0L);
        AlertStats.AlertStatsTotals totales = new AlertStats.AlertStatsTotals(11L);
        return new AlertStats(LocalDate.now(), LocalDate.now(), "DAY", totales,
                List.of(), new AlertStats.AlertStatsOpen(bySev));
    }
}
