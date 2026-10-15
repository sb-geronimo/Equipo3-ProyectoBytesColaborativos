package es.bytescolab.msdashboard.service.impl;

import es.bytescolab.msdashboard.client.AlertsClient;
import es.bytescolab.msdashboard.client.FuelClient;
import es.bytescolab.msdashboard.client.MaintenanceClient;
import es.bytescolab.msdashboard.client.RoutesClient;
import es.bytescolab.msdashboard.client.VehicleClient;
import es.bytescolab.msdashboard.config.CacheConfig;
import es.bytescolab.msdashboard.dto.response.DashboardAlertsBlock;
import es.bytescolab.msdashboard.dto.response.DashboardFleetBlock;
import es.bytescolab.msdashboard.dto.response.DashboardKpisBlock;
import es.bytescolab.msdashboard.dto.response.DashboardPeriodBlock;
import es.bytescolab.msdashboard.dto.response.DashboardResponse;
import es.bytescolab.msdashboard.dto.response.DashboardUpcomingMaintenanceItem;
import es.bytescolab.msdashboard.dto.response.client.AlertStats;
import es.bytescolab.msdashboard.dto.response.client.FuelStats;
import es.bytescolab.msdashboard.dto.response.client.MaintenanceOrdersPage;
import es.bytescolab.msdashboard.dto.response.client.MaintenanceStats;
import es.bytescolab.msdashboard.dto.response.client.RouteStats;
import es.bytescolab.msdashboard.dto.response.client.VehicleFleetSummary;
import es.bytescolab.msdashboard.service.DashboardAggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class DashboardAggregatorImpl implements DashboardAggregator {

    private static final int MAX_PROXIMOS_MANTENIMIENTOS = 10;

    private final VehicleClient vehicleClient;
    private final RoutesClient routesClient;
    private final FuelClient fuelClient;
    private final MaintenanceClient maintenanceClient;
    private final AlertsClient alertsClient;
    private final ExecutorService pool;

    public DashboardAggregatorImpl(VehicleClient vehicleClient,
                                   RoutesClient routesClient,
                                   FuelClient fuelClient,
                                   MaintenanceClient maintenanceClient,
                                   AlertsClient alertsClient) {
        this.vehicleClient = vehicleClient;
        this.routesClient = routesClient;
        this.fuelClient = fuelClient;
        this.maintenanceClient = maintenanceClient;
        this.alertsClient = alertsClient;
        this.pool = Executors.newFixedThreadPool(5, r -> {
            Thread hilo = new Thread(r, "dashboard-aggregator");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_DASHBOARD_SUMMARY, key = "#days")
    public DashboardResponse getSummary(int days) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate desde = today.minusDays(days);
        DashboardPeriodBlock periodo = new DashboardPeriodBlock(desde, today);

        List<String> noDisponibles = new ArrayList<>();

        CompletableFuture<VehicleFleetSummary> futurosVehiculos =
                CompletableFuture.supplyAsync(() -> vehicleClient.getFleetSummary(), pool);
        CompletableFuture<RouteStats> futurosRutas = CompletableFuture
                .supplyAsync(() -> routesClient.getStats(desde, today, "DAY"), pool);
        CompletableFuture<FuelStats> futurosCombustible = CompletableFuture
                .supplyAsync(() -> fuelClient.getStats(desde, today, "DAY"), pool);
        CompletableFuture<MaintenanceStats> futurosMantenimiento = CompletableFuture
                .supplyAsync(() -> maintenanceClient.getStats(desde, today, "DAY"), pool);
        CompletableFuture<AlertStats> futurosAlertas = CompletableFuture
                .supplyAsync(() -> alertsClient.getStats(desde, today, "DAY"), pool);

        DashboardFleetBlock flota = recogerFlota(futurosVehiculos, "ms-vehicles", noDisponibles);
        DashboardKpisBlock kpis = recogerKpis(
                futurosRutas, futurosCombustible, futurosMantenimiento, noDisponibles);
        DashboardAlertsBlock alertas = recogerAlertas(futurosAlertas, "ms-alerts", noDisponibles);
        List<DashboardUpcomingMaintenanceItem> proximos = recogerProximosMantenimientos(
                futuresUpcomingMaintenance(desde, today), "ms-maintenance", noDisponibles);

        DashboardResponse respuesta = new DashboardResponse(
                Instant.now(),
                periodo,
                !noDisponibles.isEmpty(),
                List.copyOf(noDisponibles),
                flota,
                kpis,
                alertas,
                proximos
        );
        log.info("Resumen dashboard generado days={} unavailable={}", days, noDisponibles);
        return respuesta;
    }

    private CompletableFuture<MaintenanceOrdersPage> futuresUpcomingMaintenance(
            LocalDate desde, LocalDate today) {
        return CompletableFuture.supplyAsync(
                () -> maintenanceClient.listOrders("PENDING", today, 0, MAX_PROXIMOS_MANTENIMIENTOS),
                pool);
    }

    private DashboardFleetBlock recogerFlota(CompletableFuture<VehicleFleetSummary> futuro,
                                             String servicio,
                                             List<String> noDisponibles) {
        try {
            VehicleFleetSummary resumen = futuro.join();
            long disponibles = contarDelMapa(resumen.byStatus(), "AVAILABLE");
            long enUso = contarDelMapa(resumen.byStatus(), "IN_USE");
            long enMantenimiento = contarDelMapa(resumen.byStatus(), "IN_MAINTENANCE");
            long fuera = contarDelMapa(resumen.byStatus(), "OUT_OF_SERVICE");
            return new DashboardFleetBlock(resumen.total(), disponibles, enUso, enMantenimiento, fuera);
        } catch (Exception ex) {
            log.warn("Servicio {} no disponible para flota: {}", servicio, ex.getMessage());
            noDisponibles.add(servicio);
            return null;
        }
    }

    private DashboardKpisBlock recogerKpis(
            CompletableFuture<RouteStats> futuroRutas,
            CompletableFuture<FuelStats> futuroCombustible,
            CompletableFuture<MaintenanceStats> futuroMantenimiento,
            List<String> noDisponibles) {
        Long rutasCompletadas = null;
        BigDecimal distanciaKm = null;
        try {
            RouteStats rutas = futuroRutas.join();
            rutasCompletadas = rutas.totals() == null ? 0L : rutas.totals().routes();
            distanciaKm = rutas.totals() == null ? BigDecimal.ZERO : rutas.totals().distanceKm();
        } catch (Exception ex) {
            log.warn("Servicio ms-routes no disponible para KPIs: {}", ex.getMessage());
            noDisponibles.add("ms-routes");
        }

        BigDecimal litros = null;
        BigDecimal coste = null;
        BigDecimal consumoMedio = null;
        try {
            FuelStats combustible = futuroCombustible.join();
            litros = combustible.totals() == null ? null : combustible.totals().liters();
            coste = combustible.totals() == null ? null : combustible.totals().cost();
            consumoMedio = combustible.totals() == null ? null : combustible.totals().avgL100km();
        } catch (Exception ex) {
            log.warn("Servicio ms-fuel no disponible para KPIs: {}", ex.getMessage());
            noDisponibles.add("ms-fuel");
        }

        BigDecimal costeMantenimiento = null;
        try {
            MaintenanceStats mantenimiento = futuroMantenimiento.join();
            costeMantenimiento = mantenimiento.totals() == null ? null : mantenimiento.totals().cost();
        } catch (Exception ex) {
            log.warn("Servicio ms-maintenance no disponible para KPIs: {}", ex.getMessage());
            noDisponibles.add("ms-maintenance");
        }

        return new DashboardKpisBlock(
                rutasCompletadas,
                distanciaKm,
                litros,
                coste,
                consumoMedio,
                costeMantenimiento
        );
    }

    private DashboardAlertsBlock recogerAlertas(CompletableFuture<AlertStats> futuro,
                                                String servicio,
                                                List<String> noDisponibles) {
        try {
            AlertStats stats = futuro.join();
            long criticas = 0L;
            long advertencias = 0L;
            long informativas = 0L;
            if (stats.open() != null && stats.open().counts() != null) {
                Map<String, Long> conteo = stats.open().counts();
                criticas = conteo.getOrDefault("CRITICAL", 0L);
                advertencias = conteo.getOrDefault("WARNING", 0L);
                informativas = conteo.getOrDefault("INFO", 0L);
            }
            long total = criticas + advertencias + informativas;
            return new DashboardAlertsBlock(total, criticas, advertencias, informativas);
        } catch (Exception ex) {
            log.warn("Servicio {} no disponible para alertas: {}", servicio, ex.getMessage());
            noDisponibles.add(servicio);
            return null;
        }
    }

    private List<DashboardUpcomingMaintenanceItem> recogerProximosMantenimientos(
            CompletableFuture<MaintenanceOrdersPage> futuro,
            String servicio,
            List<String> noDisponibles) {
        try {
            MaintenanceOrdersPage pagina = futuro.join();
            List<DashboardUpcomingMaintenanceItem> items = new ArrayList<>();
            if (pagina == null || pagina.content() == null) {
                return items;
            }
            for (MaintenanceOrdersPage.MaintenanceOrderSummary orden : pagina.content()) {
                items.add(new DashboardUpcomingMaintenanceItem(
                        parseUuid(orden.id()),
                        parseUuid(orden.vehicleId()),
                        null,
                        orden.type(),
                        orden.scheduledFor()));
            }
            items.sort(Comparator.comparing(
                    DashboardUpcomingMaintenanceItem::scheduledFor,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            return items;
        } catch (Exception ex) {
            log.warn("Servicio {} no disponible para próximos mantenimientos: {}",
                    servicio, ex.getMessage());
            noDisponibles.add(servicio);
            return List.of();
        }
    }

    private static long contarDelMapa(Map<String, Long> mapa, String clave) {
        if (mapa == null) {
            return 0L;
        }
        return mapa.getOrDefault(clave, 0L);
    }

    private static java.util.UUID parseUuid(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return java.util.UUID.fromString(valor);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
