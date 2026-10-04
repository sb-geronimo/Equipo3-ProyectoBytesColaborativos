package es.bytescolab.msvehicles.mapper;

import es.bytescolab.msvehicles.dto.request.CreateVehicleRequest;
import es.bytescolab.msvehicles.dto.response.PageResponse;
import es.bytescolab.msvehicles.dto.response.VehicleDetailsResponse;
import es.bytescolab.msvehicles.dto.response.VehicleSummaryResponse;
import es.bytescolab.msvehicles.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    VehicleSummaryResponse toSummaryResponse(Vehicle entity);

    PageResponse<VehicleSummaryResponse> toPageResponse(Page<Vehicle> page);

    @Mapping(target = "status", constant = "AVAILABLE")
    Vehicle toEntity(CreateVehicleRequest request);

    VehicleDetailsResponse toDetailsResponse(Vehicle entity);
}
