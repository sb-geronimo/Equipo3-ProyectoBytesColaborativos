package es.bytescolab.msmaintenance.service;

import es.bytescolab.msmaintenance.dto.request.MaintenanceStatsRequest;
import es.bytescolab.msmaintenance.dto.response.MaintenanceStatsResponse;
import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import es.bytescolab.msmaintenance.repository.MaintenanceOrderRepository;
import es.bytescolab.msmaintenance.service.impl.MaintenanceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceStatsServiceTest {

    @Mock
    private MaintenanceOrderRepository repository;

    @InjectMocks
    private MaintenanceServiceImpl service;

    @Test
    void cambioMes_devuelvePuntosParaCadaMes() {
        when(repository.findCompletedInRange(any(), any(), any())).thenReturn(List.of());

        MaintenanceStatsRequest request = new MaintenanceStatsRequest(
                LocalDate.of(2026, 9, 28),
                LocalDate.of(2026, 10, 5),
                "MONTH",
                null
        );

        MaintenanceStatsResponse response = service.getStats(request);

        assertThat(response.series()).hasSize(2);
        assertThat(response.series().get(0).period()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.series().get(1).period()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(response.series().get(0).orders()).isZero();
        assertThat(response.series().get(1).orders()).isZero();
    }

    @Test
    void semanaIncompleta_normalizaALunes() {
        when(repository.findCompletedInRange(any(), any(), any())).thenReturn(List.of());

        MaintenanceStatsRequest request = new MaintenanceStatsRequest(
                LocalDate.of(2026, 9, 23),
                LocalDate.of(2026, 9, 30),
                "WEEK",
                null
        );

        MaintenanceStatsResponse response = service.getStats(request);

        assertThat(response.series()).hasSize(2);
        assertThat(response.series().get(0).period()).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(response.series().get(1).period()).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    void rangoVacio_llenaCeros() {
        when(repository.findCompletedInRange(any(), any(), any())).thenReturn(List.of());

        MaintenanceStatsRequest request = new MaintenanceStatsRequest(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 10),
                "DAY",
                null
        );

        MaintenanceStatsResponse response = service.getStats(request);

        assertThat(response.totals().orders()).isZero();
        assertThat(response.totals().cost()).isEqualByComparingTo("0.00");
        assertThat(response.series()).hasSize(4);
        assertThat(response.byType()).isEmpty();
        assertThat(response.upcoming().overdue()).isZero();
        assertThat(response.upcoming().dueIn7Days()).isZero();
    }

    @Test
    void totalesIgualASumaDeSerie() {
        UUID vehicle = UUID.randomUUID();
        LocalDate dia = LocalDate.of(2026, 9, 10);
        List<MaintenanceOrder> ordenes = List.of(
                ordenCompletada(vehicle, dia, new BigDecimal("100.00")),
                ordenCompletada(vehicle, dia, new BigDecimal("250.50")),
                ordenCompletada(vehicle, dia, new BigDecimal("74.25"))
        );

        when(repository.findCompletedInRange(eq(vehicle), any(), any())).thenReturn(ordenes);

        MaintenanceStatsRequest request = new MaintenanceStatsRequest(
                dia,
                dia,
                "DAY",
                vehicle
        );

        MaintenanceStatsResponse response = service.getStats(request);

        assertThat(response.totals().orders()).isEqualTo(3L);
        assertThat(response.totals().cost()).isEqualByComparingTo("424.75");
        assertThat(response.series()).hasSize(1);
        assertThat(response.series().get(0).orders()).isEqualTo(3L);
        assertThat(response.series().get(0).cost()).isEqualByComparingTo("424.75");
        assertThat(response.byType()).hasSize(1);
        assertThat(response.byType().get(0).type()).isEqualTo(MaintenanceType.OIL_CHANGE);
    }

    @Test
    void upcomingEsSnapshotIndependienteDelRango() {
        when(repository.findCompletedInRange(any(), any(), any())).thenReturn(List.of());
        when(repository.countOverduePending(any(), any())).thenReturn(1L);
        when(repository.countDueWithinSevenDays(any(), any(), any())).thenReturn(5L);

        MaintenanceStatsRequest request = new MaintenanceStatsRequest(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "DAY",
                null
        );

        MaintenanceStatsResponse response = service.getStats(request);

        assertThat(response.upcoming().overdue()).isEqualTo(1L);
        assertThat(response.upcoming().dueIn7Days()).isEqualTo(5L);
    }

    private MaintenanceOrder ordenCompletada(UUID vehicle, LocalDate dia, BigDecimal cost) {
        Instant completedAt = dia.atStartOfDay(ZoneOffset.UTC).toInstant();
        return MaintenanceOrder.builder()
                .vehicleId(vehicle)
                .type(MaintenanceType.OIL_CHANGE)
                .status(MaintenanceOrderStatus.COMPLETED)
                .scheduledFor(dia)
                .completedAt(completedAt)
                .cost(cost)
                .build();
    }
}