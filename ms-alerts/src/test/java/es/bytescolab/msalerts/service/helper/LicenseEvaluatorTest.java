package es.bytescolab.msalerts.service.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.bytescolab.msalerts.client.DriverClient;
import es.bytescolab.msalerts.dto.response.client.DriverSummaryResponse;
import es.bytescolab.msalerts.dto.response.client.PageResponse;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LicenseEvaluatorTest {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 10, 16);

    @Mock
    private DriverClient driverClient;

    @Mock
    private Clock clock;

    @InjectMocks
    private LicenseEvaluator evaluator;

    @BeforeEach
    void fijarReloj() {
        when(clock.instant()).thenReturn(FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void evaluate_should_emitExpiringAlert_when_driverLicenseWithinWindow() {
        UUID driverId = UUID.randomUUID();
        LocalDate expiresAt = FIXED_TODAY.plusDays(15);
        DriverSummaryResponse driver = driver(driverId, "Ana López", "B-12345", expiresAt);
        when(driverClient.listDrivers(anyInt(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(driver), 0, 100, 1, 1));

        List<AlertCandidate> candidates = evaluator.evaluate(expiringRule(30));

        assertThat(candidates).hasSize(1);
        AlertCandidate candidate = candidates.get(0);
        assertThat(candidate.type()).isEqualTo(AlertType.LICENSE_EXPIRING);
        assertThat(candidate.driverId()).isEqualTo(driverId);
        assertThat(candidate.severity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(candidate.dedupKey()).isEqualTo("LICENSE_EXPIRING:" + driverId);
        assertThat(candidate.message()).contains("Ana López");
        assertThat(candidate.message()).contains(expiresAt.toString());
    }

    @Test
    void evaluate_should_emitExpiredAlert_withCriticalSeverity_when_driverLicenseIsPast() {
        UUID driverId = UUID.randomUUID();
        LocalDate expiredOn = FIXED_TODAY.minusDays(5);
        DriverSummaryResponse driver = driver(driverId, "Juan Pérez", "C-98765", expiredOn);
        when(driverClient.listDrivers(isNull(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(driver), 0, 100, 1, 1));

        List<AlertCandidate> candidates = evaluator.evaluate(expiredRule());

        assertThat(candidates).hasSize(1);
        AlertCandidate candidate = candidates.get(0);
        assertThat(candidate.type()).isEqualTo(AlertType.LICENSE_EXPIRED);
        assertThat(candidate.driverId()).isEqualTo(driverId);
        assertThat(candidate.severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(candidate.dedupKey()).isEqualTo("LICENSE_EXPIRED:" + driverId);
        assertThat(candidate.message()).contains("Juan Pérez");
        assertThat(candidate.message()).contains(expiredOn.toString());
    }

    @Test
    void evaluate_should_returnEmpty_when_noDriversMatchWindow() {
        when(driverClient.listDrivers(anyInt(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0));

        List<AlertCandidate> candidates = evaluator.evaluate(expiringRule(30));

        assertThat(candidates).isEmpty();
    }

    private static AlertRuleEntity expiringRule(int daysBefore) {
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("daysBefore", daysBefore);
        return AlertRuleEntity.builder()
                .type(AlertType.LICENSE_EXPIRING)
                .enabled(Boolean.TRUE)
                .severity(AlertSeverity.WARNING)
                .params(params)
                .updatedAt(Instant.now())
                .build();
    }

    private static AlertRuleEntity expiredRule() {
        return AlertRuleEntity.builder()
                .type(AlertType.LICENSE_EXPIRED)
                .enabled(Boolean.TRUE)
                .severity(AlertSeverity.CRITICAL)
                .params(new ObjectMapper().createObjectNode())
                .updatedAt(Instant.now())
                .build();
    }

    private static DriverSummaryResponse driver(UUID id, String fullName, String licenseNumber,
                                                 LocalDate licenseExpiresAt) {
        return new DriverSummaryResponse(id, fullName, licenseNumber, licenseExpiresAt);
    }
}
