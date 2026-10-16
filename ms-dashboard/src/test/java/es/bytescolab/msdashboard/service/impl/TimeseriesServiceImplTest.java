package es.bytescolab.msdashboard.service.impl;

import es.bytescolab.msdashboard.client.AlertsClient;
import es.bytescolab.msdashboard.client.FuelClient;
import es.bytescolab.msdashboard.client.MaintenanceClient;
import es.bytescolab.msdashboard.client.RoutesClient;
import es.bytescolab.msdashboard.dto.request.MetricsTimeseriesRequest;
import es.bytescolab.msdashboard.dto.response.TimeseriesResponse;
import es.bytescolab.msdashboard.dto.response.client.RouteStats;
import es.bytescolab.msdashboard.enums.DashboardMetric;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeseriesServiceImplTest {

    @Mock
    private RoutesClient routesClient;

    @Mock
    private FuelClient fuelClient;

    @Mock
    private MaintenanceClient maintenanceClient;

    @Mock
    private AlertsClient alertsClient;

    @Test
    void metricsVacia_lanzaIllegalArgument() {
        TimeseriesServiceImpl servicio = crearServicio();
        MetricsTimeseriesRequest request = new MetricsTimeseriesRequest(
                List.of(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10), "DAY");

        assertThatThrownBy(() -> servicio.getTimeseries(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una métrica");
    }

    @Test
    void rangoMayorA366Dias_lanzaIllegalArgument() {
        TimeseriesServiceImpl servicio = crearServicio();
        MetricsTimeseriesRequest request = new MetricsTimeseriesRequest(
                List.of(DashboardMetric.DISTANCE_KM),
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 10, 14), "DAY");

        assertThatThrownBy(() -> servicio.getTimeseries(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supera el máximo");
    }

    @Test
    void toAntesDeFrom_lanzaIllegalArgument() {
        TimeseriesServiceImpl servicio = crearServicio();
        MetricsTimeseriesRequest request = new MetricsTimeseriesRequest(
                List.of(DashboardMetric.DISTANCE_KM),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1), "DAY");

        assertThatThrownBy(() -> servicio.getTimeseries(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("igual o posterior");
    }

    @Test
    void granularidadInvalida_lanzaIllegalArgument() {
        TimeseriesServiceImpl servicio = crearServicio();
        MetricsTimeseriesRequest request = new MetricsTimeseriesRequest(
                List.of(DashboardMetric.DISTANCE_KM),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 14), "HOUR");

        assertThatThrownBy(() -> servicio.getTimeseries(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("granularidad");
    }

    @Test
    void metricasValidas_devuelveSeriesYOmiteServiciosCaidos() {
        when(routesClient.getStats(any(), any(), anyString())).thenReturn(rutasConPunto());
        when(fuelClient.getStats(any(), any(), anyString())).thenThrow(new RuntimeException("ms-fuel caído"));

        TimeseriesServiceImpl servicio = crearServicio();
        MetricsTimeseriesRequest request = new MetricsTimeseriesRequest(
                List.of(DashboardMetric.DISTANCE_KM, DashboardMetric.FUEL_COST),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 14), "DAY");

        TimeseriesResponse respuesta = servicio.getTimeseries(request);

        assertThat(respuesta.series()).extracting(s -> s.metric())
                .containsExactly(DashboardMetric.DISTANCE_KM);
        assertThat(respuesta.unavailable()).containsExactly("ms-fuel");
        verify(maintenanceClient, never()).getStats(any(), any(), anyString());
        verify(alertsClient, never()).getStats(any(), any(), anyString());
    }

    private TimeseriesServiceImpl crearServicio() {
        return new TimeseriesServiceImpl(routesClient, fuelClient, maintenanceClient, alertsClient);
    }

    private static RouteStats rutasConPunto() {
        RouteStats.RouteStatsPoint punto = new RouteStats.RouteStatsPoint(
                LocalDate.of(2026, 10, 1), 5L, new BigDecimal("123.45"), 60);
        RouteStats.RouteStatsTotals totales = new RouteStats.RouteStatsTotals(
                5L, new BigDecimal("123.45"));
        return new RouteStats(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 14),
                "DAY", totales, List.of(punto));
    }
}
