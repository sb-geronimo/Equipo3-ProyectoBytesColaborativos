package es.bytescolab.msdashboard.dto.response.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MaintenanceOrdersPage(
        List<MaintenanceOrderSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public record MaintenanceOrderSummary(
            String id,
            String planId,
            String vehicleId,
            String type,
            String status,
            LocalDate scheduledFor,
            BigDecimal cost
    ) {
    }
}
