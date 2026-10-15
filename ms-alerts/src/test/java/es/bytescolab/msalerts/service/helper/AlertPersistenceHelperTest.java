package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertPersistenceHelperTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-15T12:00:00Z");

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private AlertPersistenceHelper persistenceHelper;

    @BeforeEach
    void fijarReloj() {
        lenient().when(clock.instant()).thenReturn(FIXED_NOW);
    }

    @Test
    void reaparicionTrasResolved_insertaNuevaAlerta() {
        AlertCandidate candidate = candidateWithDedupKey("MAINTENANCE_OVERDUE:order-1");
        when(alertRepository.findActiveByDedupKeys(anyCollection())).thenReturn(List.of());
        when(alertRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        AlertPersistenceHelper.PersistenceResult result =
                persistenceHelper.apply(List.of(candidate));

        ArgumentCaptor<List<AlertEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(alertRepository).saveAll(saved.capture());
        assertThat(result.created()).isEqualTo(1);
        assertThat(saved.getValue()).hasSize(1);
        assertThat(saved.getValue().get(0).getDedupKey()).isEqualTo("MAINTENANCE_OVERDUE:order-1");
        assertThat(saved.getValue().get(0).getStatus()).isEqualTo(AlertStatus.OPEN);
    }

    @Test
    void duplicadoEnMismaRonda_insertaUnaSolaVez() {
        AlertCandidate first = candidateWithDedupKey("MAINTENANCE_DUE:order-2");
        AlertCandidate second = candidateWithDedupKey("MAINTENANCE_DUE:order-2");
        when(alertRepository.findActiveByDedupKeys(anyCollection())).thenReturn(List.of());
        when(alertRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        AlertPersistenceHelper.PersistenceResult result =
                persistenceHelper.apply(List.of(first, second));

        ArgumentCaptor<List<AlertEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(alertRepository).saveAll(saved.capture());
        assertThat(result.created()).isEqualTo(1);
        assertThat(saved.getValue()).hasSize(1);
    }

    @Test
    void activaExistente_noInserta() {
        AlertCandidate candidate = candidateWithDedupKey("LICENSE_EXPIRED:driver-1");
        AlertEntity active = AlertEntity.builder()
                .id(UUID.randomUUID())
                .type(AlertType.LICENSE_EXPIRED)
                .severity(AlertSeverity.CRITICAL)
                .status(AlertStatus.OPEN)
                .dedupKey("LICENSE_EXPIRED:driver-1")
                .message("Licencia caducada")
                .createdAt(FIXED_NOW)
                .build();
        when(alertRepository.findActiveByDedupKeys(anyCollection())).thenReturn(List.of(active));

        AlertPersistenceHelper.PersistenceResult result =
                persistenceHelper.apply(List.of(candidate));

        assertThat(result.created()).isZero();
        verify(alertRepository, never()).saveAll(anyList());
    }

    @Test
    void condicionDesaparecida_marcaResolved() {
        AlertEntity active = AlertEntity.builder()
                .id(UUID.randomUUID())
                .type(AlertType.MAINTENANCE_DUE)
                .severity(AlertSeverity.WARNING)
                .status(AlertStatus.OPEN)
                .dedupKey("MAINTENANCE_DUE:order-9")
                .message("Mantenimiento próximo")
                .createdAt(FIXED_NOW)
                .build();
        AlertCandidate other = candidateWithDedupKey("MAINTENANCE_DUE:order-10");
        when(alertRepository.findActiveByDedupKeys(anyCollection())).thenReturn(List.of(active));
        when(alertRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        AlertPersistenceHelper.PersistenceResult result =
                persistenceHelper.apply(List.of(other));

        assertThat(result.resolved()).isEqualTo(1);
        assertThat(active.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(active.getResolvedAt()).isEqualTo(FIXED_NOW);
    }

    private static AlertCandidate candidateWithDedupKey(String dedupKey) {
        return new AlertCandidate(
                AlertType.MAINTENANCE_OVERDUE, UUID.randomUUID(), null,
                dedupKey, "Mensaje de prueba", AlertSeverity.CRITICAL);
    }
}
