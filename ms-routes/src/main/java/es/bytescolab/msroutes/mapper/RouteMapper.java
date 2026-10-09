package es.bytescolab.msroutes.mapper;

import es.bytescolab.msroutes.dto.request.RouteCompleteRequest;
import es.bytescolab.msroutes.dto.request.RouteCreateRequest;
import es.bytescolab.msroutes.dto.response.PageResponse;
import es.bytescolab.msroutes.dto.response.RouteResponse;
import es.bytescolab.msroutes.entity.RouteEntity;
import es.bytescolab.msroutes.enums.RouteStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RouteMapper {

    RouteResponse toResponse(RouteEntity entity);

    PageResponse<RouteResponse> toPageResponse(Page<RouteEntity> page);

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "status", constant = "PLANNED")
    RouteEntity toEntity(RouteCreateRequest request);

    void applyCompletion(RouteCompleteRequest request, @MappingTarget RouteEntity entity);

    default RouteStatus mapStatus(String value) {
        return value == null ? null : RouteStatus.valueOf(value);
    }

    default String mapStatus(RouteStatus status) {
        return status == null ? null : status.name();
    }

    default UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
