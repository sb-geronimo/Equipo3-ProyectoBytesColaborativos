package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEvaluatorRegistryTest {

    @Mock
    private MaintenanceEvaluator maintenanceEvaluator;

    @Mock
    private LicenseEvaluator licenseEvaluator;

    @Mock
    private HighConsumptionEvaluator highConsumptionEvaluator;

    @InjectMocks
    private AlertEvaluatorRegistry evaluatorRegistry;

    @Test
    void evaluacionSinCandidatos_noMarcaSkipped() {
        AlertRuleEntity rule = AlertRuleEntity.builder()
                .type(AlertType.MAINTENANCE_DUE)
                .enabled(true)
                .build();
        when(maintenanceEvaluator.evaluate(rule)).thenReturn(List.of());

        AlertEvaluatorRegistry.EvaluationOutcome outcome = evaluatorRegistry.run(rule);

        assertThat(outcome.skipped()).isFalse();
        assertThat(outcome.candidates()).isEmpty();
    }

    @Test
    void dependenciaCaida_marcaSkipped() {
        AlertRuleEntity rule = AlertRuleEntity.builder()
                .type(AlertType.LICENSE_EXPIRING)
                .enabled(true)
                .build();
        when(licenseEvaluator.evaluate(any())).thenThrow(new RuntimeException("timeout"));

        AlertEvaluatorRegistry.EvaluationOutcome outcome = evaluatorRegistry.run(rule);

        assertThat(outcome.skipped()).isTrue();
        assertThat(outcome.candidates()).isEmpty();
    }

    @Test
    void reglaSinEvaluador_marcaSkipped() {
        AlertRuleEntity rule = AlertRuleEntity.builder().enabled(true).build();

        AlertEvaluatorRegistry.EvaluationOutcome outcome = evaluatorRegistry.run(rule);

        assertThat(outcome.skipped()).isTrue();
    }
}
