package es.bytescolab.msdashboard.service.impl;

import es.bytescolab.msdashboard.client.FuelClient;
import es.bytescolab.msdashboard.config.CacheConfig;
import es.bytescolab.msdashboard.dto.response.RankingItem;
import es.bytescolab.msdashboard.dto.response.RankingResponse;
import es.bytescolab.msdashboard.dto.response.client.FuelConsumption;
import es.bytescolab.msdashboard.enums.RankingMetric;
import es.bytescolab.msdashboard.service.RankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RankingServiceImpl implements RankingService {

    private static final int DIAS_MINIMOS = 1;
    private static final int DIAS_MAXIMOS = 365;
    private static final int LIMITE_MINIMO = 1;
    private static final int LIMITE_MAXIMO = 20;

    private final FuelClient fuelClient;

    public RankingServiceImpl(FuelClient fuelClient) {
        this.fuelClient = fuelClient;
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_DASHBOARD_RANKING,
            key = "T(java.util.Objects).hash(#metric,#days,#limit,#type)")
    public RankingResponse getRanking(RankingMetric metric, int days, int limit, String type) {
        validar(metric, days, limit);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate desde = today.minusDays(days);
        FuelConsumption consumo = fuelClient.getConsumption(desde, today);

        List<FuelConsumption.VehicleFuelConsumption> filtrados = new ArrayList<>();
        if (consumo != null && consumo.vehicles() != null) {
            for (FuelConsumption.VehicleFuelConsumption v : consumo.vehicles()) {
                if (type != null && !type.isBlank()
                        && (v.type() == null || !type.equalsIgnoreCase(v.type()))) {
                    continue;
                }
                filtrados.add(v);
            }
        }

        Comparator<FuelConsumption.VehicleFuelConsumption> comparador =
                switch (metric) {
                    case FUEL_COST -> Comparator.comparing(
                            FuelConsumption.VehicleFuelConsumption::cost,
                            Comparator.nullsLast(BigDecimal::compareTo)).reversed();
                    case CONSUMPTION -> Comparator.comparing(
                            FuelConsumption.VehicleFuelConsumption::avgL100km,
                            Comparator.nullsLast(BigDecimal::compareTo)).reversed();
                };
        filtrados.sort(comparador);

        List<RankingItem> ranking = new ArrayList<>();
        int tope = Math.min(filtrados.size(), limit);
        for (int i = 0; i < tope; i++) {
            FuelConsumption.VehicleFuelConsumption v = filtrados.get(i);
            BigDecimal valor = metric == RankingMetric.FUEL_COST ? v.cost() : v.avgL100km();
            ranking.add(new RankingItem(
                    i + 1,
                    parseUuid(v.vehicleId()),
                    v.plate(),
                    v.type(),
                    valor == null ? BigDecimal.ZERO : valor
            ));
        }

        log.info("Ranking generado metric={} days={} limit={} type={} entries={}",
                metric, days, limit, type, ranking.size());
        return new RankingResponse(metric, days, limit, type, metric.unidad(), ranking);
    }

    private static void validar(RankingMetric metric, int days, int limit) {
        if (metric == null) {
            throw new IllegalArgumentException("La métrica de ranking es obligatoria");
        }
        if (days < DIAS_MINIMOS || days > DIAS_MAXIMOS) {
            throw new IllegalArgumentException(
                    "El parámetro 'days' debe estar entre " + DIAS_MINIMOS + " y " + DIAS_MAXIMOS);
        }
        if (limit < LIMITE_MINIMO || limit > LIMITE_MAXIMO) {
            throw new IllegalArgumentException(
                    "El parámetro 'limit' debe estar entre " + LIMITE_MINIMO + " y " + LIMITE_MAXIMO);
        }
    }

    private static UUID parseUuid(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
