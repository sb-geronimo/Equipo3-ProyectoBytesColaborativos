package es.bytescolab.msalerts.service.impl;

import es.bytescolab.msalerts.dto.request.AlertStatsRequest;
import es.bytescolab.msalerts.dto.response.AlertResponse;
import es.bytescolab.msalerts.dto.response.AlertStatsBySeverity;
import es.bytescolab.msalerts.dto.response.AlertStatsOpen;
import es.bytescolab.msalerts.dto.response.AlertStatsPoint;
import es.bytescolab.msalerts.dto.response.AlertStatsResponse;
import es.bytescolab.msalerts.dto.response.AlertStatsTotals;
import es.bytescolab.msalerts.dto.response.PageResponse;
import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertStatus;
import es.bytescolab.msalerts.enums.AlertType;
import es.bytescolab.msalerts.exception.AlertNotFoundException;
import es.bytescolab.msalerts.exception.InvalidAlertStateException;
import es.bytescolab.msalerts.mapper.AlertMapper;
import es.bytescolab.msalerts.repository.AlertRepository;
import es.bytescolab.msalerts.repository.AlertSpecifications;
import es.bytescolab.msalerts.service.AlertQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertQueryServiceImpl implements AlertQueryService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> findAll(
            AlertStatus status,
            AlertSeverity severity,
            AlertType type,
            UUID vehicleId,
            int page,
            int size
    ) {
        Specification<AlertEntity> specification = AlertSpecifications.withFilters(status, severity, type, vehicleId);
        Sort sort = Sort.by(
                Sort.Order.asc("severityRank"),
                Sort.Order.desc("createdAt")
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        return PageResponse.from(alertRepository.searchAlerts(specification, pageable), alertMapper::toResponse);
    }

    @Override
    @Transactional
    public AlertResponse acknowledge(UUID alertId, String comment, UUID currentUserId) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new InvalidAlertStateException(alertId, alert.getStatus());
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        alert.setAcknowledgedBy(currentUserId);
        alert.setComment(comment);
        AlertEntity saved = alertRepository.save(alert);
        log.info("Alerta {} reconocida por {}", alertId, currentUserId);
        return alertMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertStatsResponse getStats(AlertStatsRequest request) {
        String granularity = request.granularity();

        Instant desde = request.from().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant hasta = request.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<AlertEntity> alertas = alertRepository.findCreatedInRange(desde, hasta);

        Map<LocalDate, PeriodoAcumulado> cubos = new HashMap<>();
        for (AlertEntity alerta : alertas) {
            LocalDate creadaEl = LocalDate.ofInstant(alerta.getCreatedAt(), ZoneOffset.UTC);
            LocalDate periodo = periodoDe(creadaEl, granularity);
            cubos.computeIfAbsent(periodo, k -> new PeriodoAcumulado()).add(alerta);
        }

        List<LocalDate> rellenados = rellenarPeriodos(request.from(), request.to(), granularity);
        List<AlertStatsPoint> serie = new ArrayList<>(rellenados.size());
        long totalCreadas = 0L;
        for (LocalDate periodo : rellenados) {
            PeriodoAcumulado acumulado = cubos.getOrDefault(periodo, new PeriodoAcumulado());
            serie.add(new AlertStatsPoint(
                    periodo,
                    acumulado.conteo,
                    new AlertStatsBySeverity(new EnumMap<>(acumulado.conteosPorSeveridad))
            ));
            totalCreadas += acumulado.conteo;
        }

        Map<AlertSeverity, Long> conteosAbiertos = new EnumMap<>(AlertSeverity.class);
        for (AlertSeverity severidad : AlertSeverity.values()) {
            conteosAbiertos.put(
                    severidad,
                    alertRepository.countByStatusAndSeverity(AlertStatus.OPEN, severidad));
        }

        return new AlertStatsResponse(
                request.from(),
                request.to(),
                granularity,
                new AlertStatsTotals(totalCreadas),
                serie,
                new AlertStatsOpen(conteosAbiertos)
        );
    }

    private LocalDate periodoDe(LocalDate fecha, String granularidad) {
        return switch (granularidad) {
            case "WEEK" -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "MONTH" -> fecha.withDayOfMonth(1);
            default -> fecha;
        };
    }

    private List<LocalDate> rellenarPeriodos(LocalDate desde, LocalDate hasta, String granularidad) {
        List<LocalDate> periodos = new ArrayList<>();
        LocalDate cursor = periodoDe(desde, granularidad);
        LocalDate fin = periodoDe(hasta, granularidad);
        while (!cursor.isAfter(fin)) {
            periodos.add(cursor);
            cursor = switch (granularidad) {
                case "WEEK" -> cursor.plusWeeks(1);
                case "MONTH" -> cursor.plusMonths(1);
                default -> cursor.plusDays(1);
            };
        }
        return periodos;
    }

    private static final class PeriodoAcumulado {
        private long conteo;
        private final Map<AlertSeverity, Long> conteosPorSeveridad = new EnumMap<>(AlertSeverity.class);

        PeriodoAcumulado() {
            for (AlertSeverity severidad : AlertSeverity.values()) {
                conteosPorSeveridad.put(severidad, 0L);
            }
        }

        void add(AlertEntity alerta) {
            conteo++;
            conteosPorSeveridad.merge(alerta.getSeverity(), 1L, Long::sum);
        }
    }
}
