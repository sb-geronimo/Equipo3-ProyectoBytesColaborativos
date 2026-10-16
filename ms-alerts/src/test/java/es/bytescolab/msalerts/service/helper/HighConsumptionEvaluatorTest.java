package es.bytescolab.msalerts.service.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.bytescolab.msalerts.client.FuelClient;
import es.bytescolab.msalerts.dto.response.client.ConsumptionResponse;
import es.bytescolab.msalerts.dto.response.client.VehicleConsumption;
import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighConsumptionEvaluatorTest {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 10, 16);

    @Mock
    private FuelClient fuelClient;

    @Mock
    private Clock clock;

    @InjectMocks
    private HighConsumptionEvaluator evaluator;

    @BeforeEach
    void fijarReloj() {
        when(clock.instant()).thenReturn(FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void evaluate_should_emitAlert_when_recentExceedsPreviousByThreshold() {
        UUID vehicleId = UUID.randomUUID();
        ConsumptionResponse recent = consumptionResponseOf(vehicleId, new BigDecimal("10.00"));
        ConsumptionResponse previous = consumptionResponseOf(vehicleId, new BigDecimal("5.00"));
        when(fuelClient.getConsumption(any(), any()))
                .thenReturn(recent)
                .thenReturn(previous);

        List<AlertCandidate> candidates = evaluator.evaluate(ruleWithThreshold(25));

        assertThat(candidates).hasSize(1);
        AlertCandidate candidate = candidates.get(0);
        assertThat(candidate.type()).isEqualTo(AlertType.HIGH_CONSUMPTION);
        assertThat(candidate.vehicleId()).isEqualTo(vehicleId);
        assertThat(candidate.severity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(candidate.dedupKey()).isEqualTo("HIGH_CONSUMPTION:" + vehicleId);
        assertThat(candidate.message()).contains(vehicleId.toString());
    }

    @Test
    void evaluate_should_returnEmpty_when_recentBelowThreshold() {
        UUID vehicleId = UUID.randomUUID();
        ConsumptionResponse recent = consumptionResponseOf(vehicleId, new BigDecimal("5.50"));
        ConsumptionResponse previous = consumptionResponseOf(vehicleId, new BigDecimal("5.00"));
        when(fuelClient.getConsumption(any(), any()))
                .thenReturn(recent)
                .thenReturn(previous);

        List<AlertCandidate> candidates = evaluator.evaluate(ruleWithThreshold(25));

        assertThat(candidates).isEmpty();
    }

    @Test
    void evaluate_should_returnEmpty_when_previousDataIsMissing() {
        UUID vehicleId = UUID.randomUUID();
        ConsumptionResponse recent = consumptionResponseOf(vehicleId, new BigDecimal("12.00"));
        ConsumptionResponse previous = new ConsumptionResponse(null, null, List.of());
        when(fuelClient.getConsumption(any(), any()))
                .thenReturn(recent)
                .thenReturn(previous);

        List<AlertCandidate> candidates = evaluator.evaluate(ruleWithThreshold(25));

        assertThat(candidates).isEmpty();
    }

    private static AlertRuleEntity ruleWithThreshold(int thresholdPercent) {
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("thresholdPercent", thresholdPercent);
        return AlertRuleEntity.builder()
                .type(AlertType.HIGH_CONSUMPTION)
                .enabled(Boolean.TRUE)
                .severity(AlertSeverity.WARNING)
                .params(params)
                .updatedAt(Instant.now())
                .build();
    }

    private static ConsumptionResponse consumptionResponseOf(UUID vehicleId, BigDecimal avgL100km) {
        return new ConsumptionResponse(
                FIXED_TODAY.minusDays(30),
                FIXED_TODAY,
                List.of(new VehicleConsumption(vehicleId, "1234-ABC", "CAR", new BigDecimal("100"),
                        new BigDecimal("150"), 1000, avgL100km))
        );
    }
}
