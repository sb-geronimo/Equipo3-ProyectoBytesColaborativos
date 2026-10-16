package es.bytescolab.msdashboard.service;

import es.bytescolab.msdashboard.dto.response.DashboardResponse;

public interface DashboardAggregator {

    DashboardResponse getSummary(int days);
}
