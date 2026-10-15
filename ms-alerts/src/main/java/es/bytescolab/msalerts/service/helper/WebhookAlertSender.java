package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.dto.response.AlertResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class WebhookAlertSender {

    private final RestClient.Builder restClientBuilder;
    private final String webhookUrl;
    private final int webhookTimeoutMs;

    public WebhookAlertSender(RestClient.Builder restClientBuilder,
                              @Value("${alerts.webhook.url:}") String webhookUrl,
                              @Value("${alerts.webhook.timeout-ms:5000}") int webhookTimeoutMs) {
        this.restClientBuilder = restClientBuilder;
        this.webhookUrl = webhookUrl;
        this.webhookTimeoutMs = webhookTimeoutMs;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public void send(AlertResponse payload) {
        buildClient().post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        log.debug("Alerta {} notificada al webhook", payload.id());
    }

    @Recover
    public void recover(Exception ex, AlertResponse payload) {
        log.error("Webhook no disponible tras 3 intentos para la alerta {}: {}",
                payload == null ? null : payload.id(), ex.getMessage());
    }

    private RestClient buildClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(webhookTimeoutMs);
        requestFactory.setReadTimeout(webhookTimeoutMs);
        return restClientBuilder.requestFactory(requestFactory).build();
    }
}
