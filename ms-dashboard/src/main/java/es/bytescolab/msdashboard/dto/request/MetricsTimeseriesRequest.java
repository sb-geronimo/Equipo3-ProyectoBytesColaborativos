package es.bytescolab.msdashboard.dto.request;

import es.bytescolab.msdashboard.enums.DashboardMetric;

import java.time.LocalDate;
import java.util.List;

public record MetricsTimeseriesRequest(
        List<DashboardMetric> metrics,
        LocalDate from,
        LocalDate to,
        String granularity
) {
}
