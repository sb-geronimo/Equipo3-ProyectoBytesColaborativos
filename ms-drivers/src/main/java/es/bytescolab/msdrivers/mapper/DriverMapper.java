package es.bytescolab.msdrivers.mapper;

import es.bytescolab.msdrivers.dto.request.DriverCreateRequest;
import es.bytescolab.msdrivers.dto.request.DriverUpdateRequest;
import es.bytescolab.msdrivers.dto.response.DriverDetailResponse;
import es.bytescolab.msdrivers.dto.response.DriverSummaryResponse;
import es.bytescolab.msdrivers.entity.DriverEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DriverMapper {

    DriverDetailResponse toDetailResponse(DriverEntity entity);

    DriverSummaryResponse toSummaryResponse(DriverEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    DriverEntity toEntity(DriverCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(DriverUpdateRequest request, @MappingTarget DriverEntity entity);
}
