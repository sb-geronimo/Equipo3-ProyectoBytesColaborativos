package es.bytescolab.msalerts.service.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.bytescolab.msalerts.client.MaintenanceClient;
import es.bytescolab.msalerts.client.VehicleClient;
import es.bytescolab.msalerts.dto.response.client.OrderSummaryResponse;
import es.bytescolab.msalerts.dto.response.client.PageResponse;
import es.bytescolab.msalerts.dto.response.client.VehicleDetailResponse;
import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceEvaluatorTest {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 10, 16);

    @Mock
    private MaintenanceClient maintenanceClient;

    @Mock
    private VehicleClient vehicleClient;

    @Mock
    private Clock clock;

    @InjectMocks
    private MaintenanceEvaluator evaluator;

    @BeforeEach
    void fijarReloj() {
        when(clock.instant()).thenReturn(FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void evaluate_should_emitOverdueCritical_when_scheduledBeforeToday() {
        UUID vehicleId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        LocalDate scheduledFor = FIXED_TODAY.minusDays(5);
        OrderSummaryResponse order = orderOf(orderId, vehicleId, "OIL_CHANGE", scheduledFor);
        when(maintenanceClient.listOrders("PENDING", 0, 100))
                .thenReturn(new PageResponse<>(List.of(order), 0, 100, 1, 1));
        when(vehicleClient.getVehicle(vehicleId))
                .thenReturn(new VehicleDetailResponse(vehicleId, "1234-ABC"));

        List<AlertCandidate> candidates = evaluator.evaluate(ruleWithDaysBefore(7));

        assertThat(candidates).hasSize(1);
        AlertCandidate candidate = candidates.get(0);
        assertThat(candidate.type()).isEqualTo(AlertType.MAINTENANCE_OVERDUE);
        assertThat(candidate.severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(candidate.vehicleId()).isEqualTo(vehicleId);
        assertThat(candidate.dedupKey()).isEqualTo("MAINTENANCE_OVERDUE:" + orderId);
        assertThat(candidate.message()).contains("OIL_CHANGE");
        assertThat(candidate.message()).contains("1234-ABC");
        assertThat(candidate.message()).contains("vencido desde el " + scheduledFor);
    }

    @Test
    void evaluate_should_emitDueWarning_when_scheduledWithinWindow() {
        UUID vehicleId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        LocalDate scheduledFor = FIXED_TODAY.plusDays(3);
        OrderSummaryResponse order = orderOf(orderId, vehicleId, "TIRE_ROTATION", scheduledFor);
        when(maintenanceClient.listOrders("PENDING", 0, 100))
                .thenReturn(new PageResponse<>(List.of(order), 0, 100, 1, 1));
        when(vehicleClient.getVehicle(vehicleId))
                .thenReturn(new VehicleDetailResponse(vehicleId, "5678-DEF"));

        List<AlertCandidate> candidates = evaluator.evaluate(ruleWithDaysBefore(7));

        assertThat(candidates).hasSize(1);
        AlertCandidate candidate = candidates.get(0);
        assertThat(candidate.type()).isEqualTo(AlertType.MAINTENANCE_DUE);
        assertThat(candidate.severity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(candidate.dedupKey()).isEqualTo("MAINTENANCE_DUE:" + orderId);
        assertThat(candidate.message()).contains("previsto el " + scheduledFor);
    }

    @Test
    void evaluate_should_returnEmpty_when_scheduledBeyondWindow() {
        UUID orderId = UUID.randomUUID();
        OrderSummaryResponse order = orderOf(orderId, UUID.randomUUID(), "INSPECTION", FIXED_TODAY.plusDays(30));
        when(maintenanceClient.listOrders("PENDING", 0, 100))
                .thenReturn(new PageResponse<>(List.of(order), 0, 100, 1, 1));

        List<AlertCandidate> candidates = evaluator.evaluate(ruleWithDaysBefore(7));

        assertThat(candidates).isEmpty();
    }

    private static AlertRuleEntity ruleWithDaysBefore(int daysBefore) {
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("daysBefore", daysBefore);
        return AlertRuleEntity.builder()
                .type(AlertType.MAINTENANCE_DUE)
                .enabled(Boolean.TRUE)
                .severity(AlertSeverity.WARNING)
                .params(params)
                .updatedAt(Instant.now())
                .build();
    }

    private static OrderSummaryResponse orderOf(UUID orderId, UUID vehicleId, String type, LocalDate scheduledFor) {
        return new OrderSummaryResponse(orderId, UUID.randomUUID(), vehicleId, type, scheduledFor);
    }
}
