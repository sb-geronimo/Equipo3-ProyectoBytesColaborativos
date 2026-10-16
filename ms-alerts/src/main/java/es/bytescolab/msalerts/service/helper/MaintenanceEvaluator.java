package es.bytescolab.msalerts.service.helper;

import es.bytescolab.msalerts.client.MaintenanceClient;
import es.bytescolab.msalerts.client.VehicleClient;
import es.bytescolab.msalerts.dto.response.client.OrderSummaryResponse;
import es.bytescolab.msalerts.dto.response.client.PageResponse;
import es.bytescolab.msalerts.dto.response.client.VehicleDetailResponse;
import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertSeverity;
import es.bytescolab.msalerts.enums.AlertType;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceEvaluator implements AlertEvaluator {

    private static final int PAGE_SIZE = 100;
    private static final String PENDING_STATUS = "PENDING";

    private final MaintenanceClient maintenanceClient;
    private final VehicleClient vehicleClient;
    private final Clock clock;

    @Override
    public List<AlertCandidate> evaluate(AlertRuleEntity rule) {
        int daysBefore = rule.getParams().path("daysBefore").asInt(7);
        LocalDate today = LocalDate.now(clock);
        LocalDate dueThreshold = today.plusDays(daysBefore);

        List<OrderSummaryResponse> pendingOrders = fetchAllPending();
        List<AlertCandidate> candidates = new ArrayList<>();

        for (OrderSummaryResponse order : pendingOrders) {
            LocalDate scheduledFor = order.scheduledFor();
            if (scheduledFor == null) {
                continue;
            }
            if (scheduledFor.isBefore(today)) {
                candidates.add(buildCandidate(order, AlertType.MAINTENANCE_OVERDUE, scheduledFor));
            } else if (!scheduledFor.isAfter(dueThreshold)) {
                candidates.add(buildCandidate(order, AlertType.MAINTENANCE_DUE, scheduledFor));
            }
        }
        return candidates;
    }

    private AlertCandidate buildCandidate(OrderSummaryResponse order, AlertType type, LocalDate scheduledFor) {
        String plate = fetchPlateOrUnknown(order.vehicleId());
        String message = "Mantenimiento " + order.type() + " " + describeSchedule(type, scheduledFor)
                + " en el vehículo " + plate;
        AlertSeverity severity = type == AlertType.MAINTENANCE_OVERDUE
                ? AlertSeverity.CRITICAL
                : AlertSeverity.WARNING;
        return new AlertCandidate(
                type,
                order.vehicleId(),
                null,
                AlertDedupKeyBuilder.build(type, order.id()),
                message,
                severity
        );
    }

    private String describeSchedule(AlertType type, LocalDate scheduledFor) {
        if (type == AlertType.MAINTENANCE_OVERDUE) {
            return "vencido desde el " + scheduledFor;
        }
        return "previsto el " + scheduledFor;
    }

    private List<OrderSummaryResponse> fetchAllPending() {
        List<OrderSummaryResponse> all = new ArrayList<>();
        int page = 0;
        while (true) {
            PageResponse<OrderSummaryResponse> response =
                    maintenanceClient.listOrders(PENDING_STATUS, page, PAGE_SIZE);
            all.addAll(response.content());
            if (page >= response.totalPages() - 1 || response.content().isEmpty()) {
                break;
            }
            page++;
        }
        return all;
    }

    private String fetchPlateOrUnknown(java.util.UUID vehicleId) {
        if (vehicleId == null) {
            return "desconocido";
        }
        try {
            Optional<VehicleDetailResponse> vehicle = Optional.ofNullable(vehicleClient.getVehicle(vehicleId));
            return vehicle.map(VehicleDetailResponse::plate).orElse("desconocido");
        } catch (FeignException.NotFound ex) {
            return "desconocido";
        } catch (FeignException | CallNotPermittedException | NoFallbackAvailableException ex) {
            log.warn("No se pudo obtener la placa del vehículo {}: {}", vehicleId, ex.getMessage());
            return "desconocido";
        }
    }
}