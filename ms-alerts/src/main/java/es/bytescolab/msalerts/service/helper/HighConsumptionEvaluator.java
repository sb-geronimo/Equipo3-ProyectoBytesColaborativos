package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.client.FuelClient;
import es.bytescolab.msalerts.dto.response.client.ConsumptionResponse;
import es.bytescolab.msalerts.dto.response.client.VehicleConsumption;
import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HighConsumptionEvaluator implements AlertEvaluator {

    private static final int RECENT_DAYS = 30;
    private static final int PREVIOUS_DAYS = 60;
    private static final int PERCENT_SCALE = 2;

    private final FuelClient fuelClient;
    private final Clock clock;

    @Override
    public List<AlertCandidate> evaluate(AlertRuleEntity rule) {
        int thresholdPercent = rule.getParams().path("thresholdPercent").asInt(25);
        LocalDate today = LocalDate.now(clock);
        LocalDate recentFrom = today.minusDays(RECENT_DAYS);
        LocalDate previousFrom = today.minusDays(RECENT_DAYS + PREVIOUS_DAYS);

        Map<UUID, BigDecimal> recentByVehicle = avgL100ByVehicle(fetchConsumption(recentFrom, today));
        Map<UUID, BigDecimal> previousByVehicle = avgL100ByVehicle(fetchConsumption(previousFrom, today.minusDays(RECENT_DAYS + 1)));

        BigDecimal thresholdMultiplier = BigDecimal.valueOf(100 + thresholdPercent)
                .divide(BigDecimal.valueOf(100), PERCENT_SCALE, RoundingMode.HALF_UP);

        List<AlertCandidate> candidates = new java.util.ArrayList<>();
        for (Map.Entry<UUID, BigDecimal> entry : recentByVehicle.entrySet()) {
            UUID vehicleId = entry.getKey();
            BigDecimal last30 = entry.getValue();
            BigDecimal prev60 = previousByVehicle.get(vehicleId);
            if (prev60 == null || prev60.signum() <= 0 || last30 == null) {
                continue;
            }
            BigDecimal threshold = prev60.multiply(thresholdMultiplier);
            if (last30.compareTo(threshold) > 0) {
                BigDecimal increasePercent = last30.subtract(prev60)
                        .divide(prev60, PERCENT_SCALE, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                String message = "Consumo un " + increasePercent.stripTrailingZeros().toPlainString()
                        + "% superior a la media en el vehículo " + vehicleId;
                candidates.add(new AlertCandidate(
                        AlertType.HIGH_CONSUMPTION,
                        vehicleId,
                        null,
                        AlertDedupKeyBuilder.build(AlertType.HIGH_CONSUMPTION, vehicleId),
                        message,
                        AlertSeverity.WARNING
                ));
            }
        }
        return candidates;
    }

    private ConsumptionResponse fetchConsumption(LocalDate from, LocalDate to) {
        return fuelClient.getConsumption(from, to);
    }

    private Map<UUID, BigDecimal> avgL100ByVehicle(ConsumptionResponse response) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        if (response == null || response.vehicles() == null) {
            return result;
        }
        for (VehicleConsumption v : response.vehicles()) {
            if (v.avgL100km() != null) {
                result.put(v.vehicleId(), v.avgL100km());
            }
        }
        return result;
    }
}