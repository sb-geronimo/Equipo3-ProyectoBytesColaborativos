package es.bytescolab.msdashboard.service;

import es.bytescolab.msdashboard.dto.request.MetricsTimeseriesRequest;
import es.bytescolab.msdashboard.dto.response.TimeseriesResponse;

public interface TimeseriesService {

    TimeseriesResponse getTimeseries(MetricsTimeseriesRequest request);
}
