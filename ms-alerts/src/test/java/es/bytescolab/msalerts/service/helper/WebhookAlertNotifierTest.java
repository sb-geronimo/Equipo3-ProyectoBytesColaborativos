package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.dto.response.AlertResponse;
import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.mapper.AlertMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookAlertNotifierTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-15T12:00:00Z");

    @Mock
    private WebhookAlertSender webhookAlertSender;

    @Mock
    private AlertMapper alertMapper;

    @Test
    void urlVacia_noEnviaNada() {
        WebhookAlertNotifier notifier = new WebhookAlertNotifier(webhookAlertSender, alertMapper, "");
        AlertEntity critical = criticalAlert();

        notifier.notifyCritical(List.of(critical));

        verify(webhookAlertSender, never()).send(any());
        verify(alertMapper, never()).toResponse(any());
    }

    @Test
    void urlBlanca_noEnviaNada() {
        WebhookAlertNotifier notifier = new WebhookAlertNotifier(webhookAlertSender, alertMapper, "   ");

        notifier.notifyCritical(List.of(criticalAlert()));

        verify(webhookAlertSender, never()).send(any());
    }

    @Test
    void sinAlertas_noEnviaNada() {
        WebhookAlertNotifier notifier = new WebhookAlertNotifier(webhookAlertSender, alertMapper,
                "http://localhost:19095/hook");

        notifier.notifyCritical(List.of());
        notifier.notifyCritical(null);

        verify(webhookAlertSender, never()).send(any());
    }

    @Test
    void conUrl_enviaCadaAlertaMapeada() {
        WebhookAlertNotifier notifier = new WebhookAlertNotifier(webhookAlertSender, alertMapper,
                "http://localhost:19095/hook");
        AlertEntity first = criticalAlert();
        AlertEntity second = criticalAlert();
        AlertResponse firstPayload = responseOf(first);
        AlertResponse secondPayload = responseOf(second);
        when(alertMapper.toResponse(first)).thenReturn(firstPayload);
        when(alertMapper.toResponse(second)).thenReturn(secondPayload);

        notifier.notifyCritical(List.of(first, second));

        verify(webhookAlertSender).send(firstPayload);
        verify(webhookAlertSender).send(secondPayload);
    }

    @Test
    void falloDeEnvio_noPropagaYContinuaConLasDemas() {
        WebhookAlertNotifier notifier = new WebhookAlertNotifier(webhookAlertSender, alertMapper,
                "http://localhost:19095/hook");
        AlertEntity first = criticalAlert();
        AlertEntity second = criticalAlert();
        AlertResponse firstPayload = responseOf(first);
        AlertResponse secondPayload = responseOf(second);
        when(alertMapper.toResponse(first)).thenReturn(firstPayload);
        when(alertMapper.toResponse(second)).thenReturn(secondPayload);
        doThrow(new RuntimeException("webhook caído")).when(webhookAlertSender).send(firstPayload);

        notifier.notifyCritical(List.of(first, second));

        verify(webhookAlertSender).send(firstPayload);
        verify(webhookAlertSender).send(secondPayload);
    }

    private static AlertEntity criticalAlert() {
        return AlertEntity.builder()
                .id(UUID.randomUUID())
                .type(AlertType.MAINTENANCE_OVERDUE)
                .severity(AlertSeverity.CRITICAL)
                .status(AlertStatus.OPEN)
                .vehicleId(UUID.randomUUID())
                .message("Mantenimiento vencido")
                .dedupKey("MAINTENANCE_OVERDUE:" + UUID.randomUUID())
                .createdAt(FIXED_NOW)
                .build();
    }

    private static AlertResponse responseOf(AlertEntity alert) {
        return new AlertResponse(
                alert.getId(), alert.getType(), alert.getSeverity(), alert.getStatus(),
                alert.getVehicleId(), alert.getDriverId(), alert.getMessage(), alert.getCreatedAt(),
                null, null, null, null);
    }
}
