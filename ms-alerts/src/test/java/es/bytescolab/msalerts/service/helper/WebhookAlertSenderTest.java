package es.bytescolab.msalerts.service.helper;

import com.sun.net.httpserver.HttpServer;
import es.bytescolab.msalerts.dto.response.AlertResponse;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WebhookAlertSenderTest.RetryConfig.class)
class WebhookAlertSenderTest {

    private static final HttpServer STUB;
    private static final String STUB_URL;
    private static final AtomicInteger REQUESTS = new AtomicInteger();
    private static volatile int statusToReturn = 200;
    private static volatile String lastBody;

    static {
        try {
            STUB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            STUB.createContext("/hook", exchange -> {
                REQUESTS.incrementAndGet();
                lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(statusToReturn, -1);
                exchange.close();
            });
            STUB.start();
            STUB_URL = "http://127.0.0.1:" + STUB.getAddress().getPort() + "/hook";
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Configuration
    @EnableRetry
    static class RetryConfig {
        @Bean
        WebhookAlertSender webhookAlertSender() {
            return new WebhookAlertSender(RestClient.builder(), STUB_URL, 2000);
        }
    }

    @Autowired
    private WebhookAlertSender webhookAlertSender;

    @BeforeEach
    void reiniciarContadores() {
        REQUESTS.set(0);
        lastBody = null;
        statusToReturn = 200;
    }

    @AfterAll
    static void detenerStub() {
        STUB.stop(0);
    }

    @Test
    void exito_enviaUnSoloPostConElJsonDeLaAlerta() {
        AlertResponse payload = payload();

        assertThatNoException().isThrownBy(() -> webhookAlertSender.send(payload));

        assertThat(REQUESTS.get()).isEqualTo(1);
        assertThat(lastBody).contains(payload.id().toString());
    }

    @Test
    void falloPersistente_reintentaTresVecesYSinPropagar() {
        statusToReturn = 500;
        AlertResponse payload = payload();

        assertThatNoException().isThrownBy(() -> webhookAlertSender.send(payload));

        assertThat(REQUESTS.get()).isEqualTo(3);
    }

    private static AlertResponse payload() {
        return new AlertResponse(
                UUID.randomUUID(), AlertType.MAINTENANCE_OVERDUE, AlertSeverity.CRITICAL, AlertStatus.OPEN,
                UUID.randomUUID(), null, "Mantenimiento vencido", Instant.parse("2026-10-15T12:00:00Z"),
                null, null, null, null);
    }
}
