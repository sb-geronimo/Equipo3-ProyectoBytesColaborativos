package es.bytescolab.msalerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.bytescolab.msalerts.client.DriverClient;
import es.bytescolab.msalerts.client.FuelClient;
import es.bytescolab.msalerts.client.MaintenanceClient;
import es.bytescolab.msalerts.client.VehicleClient;
import es.bytescolab.msalerts.dto.response.client.ConsumptionResponse;
import es.bytescolab.msalerts.dto.response.client.VehicleConsumption;
import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.repository.AlertRepository;
import es.bytescolab.msalerts.repository.AlertRuleRepository;
import es.bytescolab.msalerts.service.AlertEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "internal.api-key=test-internal-key",
                "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtaG1hYy1zaWduaW5n",
                "alerts.webhook.url=",
                "alerts.webhook.timeout-ms=1000",
                "clients.vehicle-service=http://localhost:1",
                "clients.driver-service=http://localhost:1",
                "clients.maintenance-service=http://localhost:1",
                "clients.fuel-service=http://localhost:1",
                "alerts.evaluation.initial-delay-ms=3600000",
                "alerts.evaluation.interval-ms=3600000"
        }
)
@Import(TestcontainersConfiguration.class)
class AlertsEvaluatorIT {

    @Autowired
    private AlertEvaluationService alertEvaluationService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @MockitoBean
    private FuelClient fuelClient;

    @MockitoBean
    private DriverClient driverClient;

    @MockitoBean
    private MaintenanceClient maintenanceClient;

    @MockitoBean
    private VehicleClient vehicleClient;

    @BeforeEach
    void limpiarEstado() {
        alertRepository.deleteAll();
        alertRuleRepository.deleteAll();
    }

    @Test
    void evaluatePipeline_should_persistHighConsumptionAlert_when_ruleFires() {
        UUID vehicleId = UUID.randomUUID();
        persistirReglaHighConsumption(25);

        ConsumptionResponse recentWindow = new ConsumptionResponse(null, null,
                List.of(new VehicleConsumption(vehicleId, "1234-ABC", "CAR",
                        new BigDecimal("100.00"), new BigDecimal("150.00"), 1000, new BigDecimal("10.00"))));
        ConsumptionResponse previousWindow = new ConsumptionResponse(null, null,
                List.of(new VehicleConsumption(vehicleId, "1234-ABC", "CAR",
                        new BigDecimal("50.00"), new BigDecimal("75.00"), 500, new BigDecimal("5.00"))));
        when(fuelClient.getConsumption(any(), any()))
                .thenReturn(recentWindow)
                .thenReturn(previousWindow);

        var summary = alertEvaluationService.runEvaluation();

        assertThat(summary.created()).isEqualTo(1);
        assertThat(summary.resolved()).isZero();
        assertThat(summary.skipped()).doesNotContain(AlertType.HIGH_CONSUMPTION.name());

        List<AlertEntity> alertas = alertRepository.findAll();
        assertThat(alertas).hasSize(1);
        AlertEntity alerta = alertas.get(0);
        assertThat(alerta.getType()).isEqualTo(AlertType.HIGH_CONSUMPTION);
        assertThat(alerta.getSeverity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(alerta.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(alerta.getVehicleId()).isEqualTo(vehicleId);
        assertThat(alerta.getDriverId()).isNull();
        assertThat(alerta.getDedupKey()).isEqualTo("HIGH_CONSUMPTION:" + vehicleId);
        assertThat(alerta.getMessage()).contains(vehicleId.toString());
        assertThat(alerta.getCreatedAt()).isNotNull();
    }

    @Test
    void evaluatePipeline_should_persistZeroAlerts_when_noRulesPresent() {
        var summary = alertEvaluationService.runEvaluation();

        assertThat(summary.created()).isZero();
        assertThat(summary.resolved()).isZero();
        assertThat(summary.skipped()).isEmpty();
        assertThat(alertRepository.count()).isZero();
    }

    private void persistirReglaHighConsumption(int thresholdPercent) {
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("thresholdPercent", thresholdPercent);
        AlertRuleEntity regla = AlertRuleEntity.builder()
                .type(AlertType.HIGH_CONSUMPTION)
                .enabled(Boolean.TRUE)
                .severity(AlertSeverity.WARNING)
                .params(params)
                .updatedAt(Instant.now())
                .build();
        alertRuleRepository.save(regla);
    }
}
