package es.bytescolab.msroutes.mapper;

import es.bytescolab.msroutes.dto.request.CreateRouteRequest;
import es.bytescolab.msroutes.dto.response.RouteDetailResponse;
import es.bytescolab.msroutes.dto.response.RouteSummaryResponse;
import es.bytescolab.msroutes.entity.RouteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RouteMapper {

    RouteDetailResponse toDetailResponse(RouteEntity entity);

    RouteSummaryResponse toSummaryResponse(RouteEntity entity);

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "status", constant = "PLANNED")
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "startOdometerKm", ignore = true)
    @Mapping(target = "endOdometerKm", ignore = true)
    @Mapping(target = "actualDistanceKm", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isNew", ignore = true)
    RouteEntity toEntity(CreateRouteRequest request);
}