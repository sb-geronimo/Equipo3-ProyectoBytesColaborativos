package es.bytescolab.msroutes.service;

import es.bytescolab.msroutes.dto.request.RouteCompleteRequest;
import es.bytescolab.msroutes.dto.request.RouteCreateRequest;
import es.bytescolab.msroutes.dto.request.RouteStatsRequest;
import es.bytescolab.msroutes.dto.response.RouteResponse;
import es.bytescolab.msroutes.dto.response.RouteStatsResponse;
import es.bytescolab.msroutes.enums.RouteStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface RouteService {

    RouteResponse create(RouteCreateRequest request, UUID userId);

    Page<RouteResponse> findAll(UUID vehicle, UUID driver, RouteStatus status,
                                LocalDate from, LocalDate to, int page, int size);

    RouteResponse findById(UUID routeId);

    RouteResponse start(UUID routeId);

    RouteResponse complete(UUID routeId, RouteCompleteRequest request);

    RouteStatsResponse getStats(RouteStatsRequest request);
}
