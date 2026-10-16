package es.bytescolab.msdashboard.service.impl;

import es.bytescolab.msdashboard.client.AlertsClient;
import es.bytescolab.msdashboard.client.FuelClient;
import es.bytescolab.msdashboard.client.MaintenanceClient;
import es.bytescolab.msdashboard.client.RoutesClient;
import es.bytescolab.msdashboard.config.CacheConfig;
import es.bytescolab.msdashboard.dto.request.MetricsTimeseriesRequest;
import es.bytescolab.msdashboard.dto.response.TimeseriesPoint;
import es.bytescolab.msdashboard.dto.response.TimeseriesResponse;
import es.bytescolab.msdashboard.dto.response.TimeseriesSeries;
import es.bytescolab.msdashboard.dto.response.client.AlertStats;
import es.bytescolab.msdashboard.dto.response.client.FuelStats;
import es.bytescolab.msdashboard.dto.response.client.MaintenanceStats;
import es.bytescolab.msdashboard.dto.response.client.RouteStats;
import es.bytescolab.msdashboard.enums.DashboardMetric;
import es.bytescolab.msdashboard.service.TimeseriesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class TimeseriesServiceImpl implements TimeseriesService {

    private static final long DIAS_MAXIMOS = 366L;

    private final RoutesClient routesClient;
    private final FuelClient fuelClient;
    private final MaintenanceClient maintenanceClient;
    private final AlertsClient alertsClient;
    private final ExecutorService pool;

    public TimeseriesServiceImpl(RoutesClient routesClient,
                                 FuelClient fuelClient,
                                 MaintenanceClient maintenanceClient,
                                 AlertsClient alertsClient) {
        this.routesClient = routesClient;
        this.fuelClient = fuelClient;
        this.maintenanceClient = maintenanceClient;
        this.alertsClient = alertsClient;
        this.pool = Executors.newFixedThreadPool(4, r -> {
            Thread hilo = new Thread(r, "dashboard-timeseries");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_DASHBOARD_TIMESERIES,
            key = "T(java.util.Objects).hash(#request.metrics,#request.from,#request.to,#request.granularity)")
    public TimeseriesResponse getTimeseries(MetricsTimeseriesRequest request) {
        validar(request);

        Set<DashboardMetric> metricas = EnumSet.copyOf(request.metrics());
        List<String> noDisponibles = new ArrayList<>();
        List<TimeseriesSeries> series = new ArrayList<>();

        CompletableFuture<RouteStats> futuroRutas = necesitaServicio(metricas,
                DashboardMetric.DISTANCE_KM)
                ? CompletableFuture.supplyAsync(
                        () -> routesClient.getStats(request.from(), request.to(), request.granularity()),
                        pool)
                : null;
        CompletableFuture<FuelStats> futuroCombustible = necesitaServicioFuel(metricas)
                ? CompletableFuture.supplyAsync(
                        () -> fuelClient.getStats(request.from(), request.to(), request.granularity()),
                        pool)
                : null;
        CompletableFuture<MaintenanceStats> futuroMantenimiento = necesitaServicio(metricas,
                DashboardMetric.MAINTENANCE_COST)
                ? CompletableFuture.supplyAsync(
                        () -> maintenanceClient.getStats(request.from(), request.to(), request.granularity()),
                        pool)
                : null;
        CompletableFuture<AlertStats> futuroAlertas = necesitaServicio(metricas,
                DashboardMetric.ALERTS_CREATED)
                ? CompletableFuture.supplyAsync(
                        () -> alertsClient.getStats(request.from(), request.to(), request.granularity()),
                        pool)
                : null;

        if (futuroRutas != null && metricas.contains(DashboardMetric.DISTANCE_KM)) {
            try {
                RouteStats rs = futuroRutas.join();
                List<TimeseriesPoint> puntos = rs.series().stream()
                        .map(p -> new TimeseriesPoint(p.period(),
                                p.distanceKm() == null ? BigDecimal.ZERO : p.distanceKm()))
                        .toList();
                series.add(new TimeseriesSeries(DashboardMetric.DISTANCE_KM,
                        DashboardMetric.DISTANCE_KM.unidad(), puntos));
            } catch (Exception ex) {
                log.warn("Servicio ms-routes no disponible para timeseries: {}", ex.getMessage());
                noDisponibles.add("ms-routes");
            }
        }

        if (futuroCombustible != null) {
            FuelStats fs = null;
            try {
                fs = futuroCombustible.join();
            } catch (Exception ex) {
                log.warn("Servicio ms-fuel no disponible para timeseries: {}", ex.getMessage());
                noDisponibles.add("ms-fuel");
            }
            if (fs != null) {
                if (metricas.contains(DashboardMetric.FUEL_LITERS)) {
                    List<TimeseriesPoint> puntos = fs.series().stream()
                            .map(p -> new TimeseriesPoint(p.period(),
                                    p.liters() == null ? BigDecimal.ZERO : p.liters()))
                            .toList();
                    series.add(new TimeseriesSeries(DashboardMetric.FUEL_LITERS,
                            DashboardMetric.FUEL_LITERS.unidad(), puntos));
                }
                if (metricas.contains(DashboardMetric.FUEL_COST)) {
                    List<TimeseriesPoint> puntos = fs.series().stream()
                            .map(p -> new TimeseriesPoint(p.period(),
                                    p.cost() == null ? BigDecimal.ZERO : p.cost()))
                            .toList();
                    series.add(new TimeseriesSeries(DashboardMetric.FUEL_COST,
                            DashboardMetric.FUEL_COST.unidad(), puntos));
                }
                if (metricas.contains(DashboardMetric.CONSUMPTION_L100KM)) {
                    List<TimeseriesPoint> puntos = new ArrayList<>();
                    for (FuelStats.FuelStatsPoint p : fs.series()) {
                        BigDecimal valor = p.avgL100km() != null
                                ? p.avgL100km()
                                : fs.totals() == null ? null : fs.totals().avgL100km();
                        puntos.add(new TimeseriesPoint(p.period(),
                                valor == null ? BigDecimal.ZERO : valor));
                    }
                    series.add(new TimeseriesSeries(DashboardMetric.CONSUMPTION_L100KM,
                            DashboardMetric.CONSUMPTION_L100KM.unidad(), puntos));
                }
            }
        }

        if (futuroMantenimiento != null && metricas.contains(DashboardMetric.MAINTENANCE_COST)) {
            try {
                MaintenanceStats ms = futuroMantenimiento.join();
                List<TimeseriesPoint> puntos = ms.series().stream()
                        .map(p -> new TimeseriesPoint(p.period(),
                                p.cost() == null ? BigDecimal.ZERO : p.cost()))
                        .toList();
                series.add(new TimeseriesSeries(DashboardMetric.MAINTENANCE_COST,
                        DashboardMetric.MAINTENANCE_COST.unidad(), puntos));
            } catch (Exception ex) {
                log.warn("Servicio ms-maintenance no disponible para timeseries: {}", ex.getMessage());
                noDisponibles.add("ms-maintenance");
            }
        }

        if (futuroAlertas != null && metricas.contains(DashboardMetric.ALERTS_CREATED)) {
            try {
                AlertStats as = futuroAlertas.join();
                List<TimeseriesPoint> puntos = as.series().stream()
                        .map(p -> new TimeseriesPoint(p.period(),
                                BigDecimal.valueOf(p.created())))
                        .toList();
                series.add(new TimeseriesSeries(DashboardMetric.ALERTS_CREATED,
                        DashboardMetric.ALERTS_CREATED.unidad(), puntos));
            } catch (Exception ex) {
                log.warn("Servicio ms-alerts no disponible para timeseries: {}", ex.getMessage());
                noDisponibles.add("ms-alerts");
            }
        }

        return new TimeseriesResponse(
                request.from(),
                request.to(),
                request.granularity(),
                series,
                List.copyOf(noDisponibles)
        );
    }

    private static boolean necesitaServicio(Set<DashboardMetric> metricas, DashboardMetric clave) {
        return metricas.contains(clave);
    }

    private static boolean necesitaServicioFuel(Set<DashboardMetric> metricas) {
        return metricas.contains(DashboardMetric.FUEL_LITERS)
                || metricas.contains(DashboardMetric.FUEL_COST)
                || metricas.contains(DashboardMetric.CONSUMPTION_L100KM);
    }

    private static void validar(MetricsTimeseriesRequest request) {
        if (request.metrics() == null || request.metrics().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una métrica soportada");
        }
        for (DashboardMetric m : request.metrics()) {
            if (!EnumSet.allOf(DashboardMetric.class).contains(m)) {
                throw new IllegalArgumentException("La métrica '" + m + "' no está soportada");
            }
        }
        if (request.from() == null || request.to() == null) {
            throw new IllegalArgumentException("Las fechas 'from' y 'to' son obligatorias");
        }
        if (request.to().isBefore(request.from())) {
            throw new IllegalArgumentException("'to' debe ser igual o posterior a 'from'");
        }
        long dias = ChronoUnit.DAYS.between(request.from(), request.to());
        if (dias > DIAS_MAXIMOS) {
            throw new IllegalArgumentException(
                    "El rango solicitado (" + dias + " días) supera el máximo permitido ("
                            + DIAS_MAXIMOS + " días)");
        }
        String g = request.granularity();
        if (g != null && !g.isBlank()) {
            String normalizada = g.toUpperCase();
            if (!normalizada.equals("DAY") && !normalizada.equals("WEEK")
                    && !normalizada.equals("MONTH")) {
                throw new IllegalArgumentException(
                        "La granularidad '" + g + "' no está soportada (DAY, WEEK, MONTH)");
            }
        }
    }
}
