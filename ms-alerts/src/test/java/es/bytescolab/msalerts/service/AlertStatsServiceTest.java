package es.bytescolab.msalerts.service;

import es.bytescolab.msalerts.dto.request.AlertStatsRequest;
import es.bytescolab.msalerts.dto.response.AlertStatsPoint;
import es.bytescolab.msalerts.dto.response.AlertStatsResponse;
import es.bytescolab.msalerts.dto.response.AlertStatsOpen;
import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.repository.AlertRepository;
import es.bytescolab.msalerts.service.impl.AlertQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertStatsServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertQueryServiceImpl service;

    @BeforeEach
    void prepararRepositorioVacio() {
        lenient().when(alertRepository.findCreatedInRange(any(), any())).thenReturn(new ArrayList<>());
        for (AlertSeverity severity : AlertSeverity.values()) {
            lenient().when(alertRepository.countByStatusAndSeverity(eq(AlertStatus.OPEN), eq(severity)))
                    .thenReturn(0L);
        }
    }

    @Test
    void cambioMes_devuelvePuntosParaCadaMes() {
        AlertStatsRequest request = new AlertStatsRequest(
                LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 5), "MONTH");

        AlertStatsResponse response = service.getStats(request);

        List<LocalDate> periodos = response.series().stream()
                .map(AlertStatsPoint::period)
                .toList();
        assertThat(periodos).contains(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));
        assertThat(response.totals().created()).isZero();
    }

    @Test
    void semanaIncompleta_normalizaALunes() {
        AlertStatsRequest request = new AlertStatsRequest(
                LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 30), "WEEK");

        AlertStatsResponse response = service.getStats(request);

        List<LocalDate> periodos = response.series().stream()
                .map(AlertStatsPoint::period)
                .toList();
        assertThat(periodos).contains(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 28));
    }

    @Test
    void rangoVacio_llenaCeros() {
        AlertStatsRequest request = new AlertStatsRequest(
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 9), "DAY");

        AlertStatsResponse response = service.getStats(request);

        assertThat(response.totals().created()).isZero();
        assertThat(response.series()).hasSize(3);
        for (AlertStatsPoint punto : response.series()) {
            assertThat(punto.created()).isZero();
            assertThat(punto.bySeverity().counts()).containsOnlyKeys(
                    AlertSeverity.INFO, AlertSeverity.WARNING, AlertSeverity.CRITICAL);
            assertThat(punto.bySeverity().counts().values()).containsOnly(0L);
        }
        Map<AlertSeverity, Long> abiertos = response.open().counts();
        assertThat(abiertos).containsOnlyKeys(
                AlertSeverity.INFO, AlertSeverity.WARNING, AlertSeverity.CRITICAL);
        assertThat(abiertos.values()).containsOnly(0L);
    }

    @Test
    void totalesIgualASumaDeSerie() {
        List<AlertEntity> alertas = List.of(
                alerta(Instant.parse("2026-09-07T03:00:00Z"), AlertSeverity.WARNING),
                alerta(Instant.parse("2026-09-07T18:00:00Z"), AlertSeverity.CRITICAL),
                alerta(Instant.parse("2026-09-08T00:00:00Z"), AlertSeverity.WARNING));
        when(alertRepository.findCreatedInRange(any(), any())).thenReturn(alertas);

        AlertStatsRequest request = new AlertStatsRequest(
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8), "DAY");

        AlertStatsResponse response = service.getStats(request);

        long sumaSerie = response.series().stream().mapToLong(AlertStatsPoint::created).sum();
        assertThat(response.totals().created()).isEqualTo(3);
        assertThat(response.totals().created()).isEqualTo(sumaSerie);
    }

    @Test
    void bySeverityPorPeriodo_esExhaustivo() {
        List<AlertEntity> alertas = List.of(
                alerta(Instant.parse("2026-09-07T00:00:00Z"), AlertSeverity.INFO),
                alerta(Instant.parse("2026-09-08T00:00:00Z"), AlertSeverity.CRITICAL),
                alerta(Instant.parse("2026-09-08T00:00:00Z"), AlertSeverity.CRITICAL));
        when(alertRepository.findCreatedInRange(any(), any())).thenReturn(alertas);

        AlertStatsRequest request = new AlertStatsRequest(
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 9), "DAY");

        AlertStatsResponse response = service.getStats(request);

        AlertStatsPoint primero = response.series().get(0);
        Map<AlertSeverity, Long> conteosPrimero = primero.bySeverity().counts();
        assertThat(conteosPrimero).containsOnlyKeys(
                AlertSeverity.INFO, AlertSeverity.WARNING, AlertSeverity.CRITICAL);
        assertThat(conteosPrimero.get(AlertSeverity.INFO)).isEqualTo(1L);
        assertThat(conteosPrimero.get(AlertSeverity.WARNING)).isZero();
        assertThat(conteosPrimero.get(AlertSeverity.CRITICAL)).isZero();

        AlertStatsPoint segundo = response.series().get(1);
        Map<AlertSeverity, Long> conteosSegundo = segundo.bySeverity().counts();
        assertThat(conteosSegundo.get(AlertSeverity.CRITICAL)).isEqualTo(2L);

        AlertStatsPoint tercero = response.series().get(2);
        Map<AlertSeverity, Long> conteosTercero = tercero.bySeverity().counts();
        assertThat(conteosTercero).containsOnlyKeys(
                AlertSeverity.INFO, AlertSeverity.WARNING, AlertSeverity.CRITICAL);
        assertThat(conteosTercero.values()).containsOnly(0L);
    }

    @Test
    void openEsSnapshotIndependienteDelRango() {
        Map<AlertSeverity, Long> conteosAbiertos = new EnumMap<>(AlertSeverity.class);
        conteosAbiertos.put(AlertSeverity.INFO, 5L);
        conteosAbiertos.put(AlertSeverity.WARNING, 9L);
        conteosAbiertos.put(AlertSeverity.CRITICAL, 2L);
        for (Map.Entry<AlertSeverity, Long> entrada : conteosAbiertos.entrySet()) {
            when(alertRepository.countByStatusAndSeverity(
                    eq(AlertStatus.OPEN), eq(entrada.getKey())))
                    .thenReturn(entrada.getValue());
        }

        AlertStatsRequest request = new AlertStatsRequest(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "DAY");

        AlertStatsResponse response = service.getStats(request);

        AlertStatsOpen abiertos = response.open();
        assertThat(abiertos.counts().get(AlertSeverity.INFO)).isEqualTo(5L);
        assertThat(abiertos.counts().get(AlertSeverity.WARNING)).isEqualTo(9L);
        assertThat(abiertos.counts().get(AlertSeverity.CRITICAL)).isEqualTo(2L);
    }

    private static AlertEntity alerta(Instant creadoEn, AlertSeverity severidad) {
        return AlertEntity.builder()
                .id(java.util.UUID.randomUUID())
                .type(AlertType.MAINTENANCE_DUE)
                .severity(severidad)
                .status(AlertStatus.OPEN)
                .message("test")
                .dedupKey(java.util.UUID.randomUUID().toString())
                .createdAt(creadoEn)
                .build();
    }
}
