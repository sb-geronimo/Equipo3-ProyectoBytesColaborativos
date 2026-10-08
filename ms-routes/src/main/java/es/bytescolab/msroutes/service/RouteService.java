package es.bytescolab.msroutes.service;

import es.bytescolab.msroutes.dto.request.CreateRouteRequest;
import es.bytescolab.msroutes.dto.response.PageResponse;
import es.bytescolab.msroutes.dto.response.RouteDetailResponse;
import es.bytescolab.msroutes.dto.response.RouteSummaryResponse;
import es.bytescolab.msroutes.enums.RouteStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface RouteService {

    RouteDetailResponse create(CreateRouteRequest request);

    RouteDetailResponse findById(UUID routeId);

    PageResponse<RouteSummaryResponse> findAll(
            UUID vehicleId,
            UUID driverId,
            RouteStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );
}