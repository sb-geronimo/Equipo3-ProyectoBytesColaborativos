package es.bytescolab.msalerts.seeder;

import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRuleSeeder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AlertRuleRepository alertRuleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (alertRuleRepository.count() > 0) {
            log.info("Reglas de alertas: la tabla alert_rules ya contiene datos. Se omite el seeder.");
            return;
        }

        log.info("Iniciando seeder de reglas de evaluación de alertas");
        Instant now = Instant.now();

        List<AlertRuleEntity> defaults = List.of(
                buildRule(AlertType.MAINTENANCE_DUE, AlertSeverity.WARNING, paramsWithDaysBefore(7), now),
                buildRule(AlertType.MAINTENANCE_OVERDUE, AlertSeverity.CRITICAL, emptyParams(), now),
                buildRule(AlertType.LICENSE_EXPIRING, AlertSeverity.WARNING, paramsWithDaysBefore(30), now),
                buildRule(AlertType.LICENSE_EXPIRED, AlertSeverity.CRITICAL, emptyParams(), now),
                buildRule(AlertType.HIGH_CONSUMPTION, AlertSeverity.WARNING, paramsWithThresholdPercent(25), now)
        );

        alertRuleRepository.saveAll(defaults);
        log.info("Seeder de reglas de alertas completado: {} reglas insertadas", defaults.size());
    }

    private static AlertRuleEntity buildRule(
            AlertType type, AlertSeverity severity, ObjectNode params, Instant updatedAt) {
        return AlertRuleEntity.builder()
                .type(type)
                .enabled(Boolean.TRUE)
                .severity(severity)
                .params(params)
                .updatedAt(updatedAt)
                .build();
    }

    private static ObjectNode emptyParams() {
        return MAPPER.createObjectNode();
    }

    private static ObjectNode paramsWithDaysBefore(int days) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("daysBefore", days);
        return node;
    }

    private static ObjectNode paramsWithThresholdPercent(int percent) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("thresholdPercent", percent);
        return node;
    }
}