package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.mapper.AlertMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class WebhookAlertNotifier {

    private final WebhookAlertSender webhookAlertSender;
    private final AlertMapper alertMapper;
    private final String webhookUrl;

    public WebhookAlertNotifier(WebhookAlertSender webhookAlertSender,
                                AlertMapper alertMapper,
                                @Value("${alerts.webhook.url:}") String webhookUrl) {
        this.webhookAlertSender = webhookAlertSender;
        this.alertMapper = alertMapper;
        this.webhookUrl = webhookUrl;
    }

    public void notifyCritical(List<AlertEntity> criticalAlerts) {
        if (criticalAlerts == null || criticalAlerts.isEmpty()) {
            return;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Webhook deshabilitado (WEBHOOK_URL vacía): se omiten {} alertas CRITICAL",
                    criticalAlerts.size());
            return;
        }
        for (AlertEntity alert : criticalAlerts) {
            try {
                webhookAlertSender.send(alertMapper.toResponse(alert));
            } catch (Exception ex) {
                log.warn("No se pudo notificar la alerta {} al webhook: {}",
                        alert.getId(), ex.getMessage());
            }
        }
    }
}
