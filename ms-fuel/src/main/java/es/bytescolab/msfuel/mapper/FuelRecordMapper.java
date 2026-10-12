package es.bytescolab.msfuel.mapper;

import es.bytescolab.msfuel.dto.request.RefuelCreateRequest;
import es.bytescolab.msfuel.dto.response.RefuelResponse;
import es.bytescolab.msfuel.entity.FuelRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.RoundingMode;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface FuelRecordMapper {

    RefuelResponse toResponse(FuelRecordEntity entity);

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "fuelType", ignore = true)
    @Mapping(target = "totalCost", expression =
            "java(request.liters().multiply(request.pricePerLiter())"
                    + ".setScale(2, java.math.RoundingMode.HALF_UP))")
    @Mapping(target = "consumptionL100km", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    FuelRecordEntity toEntity(RefuelCreateRequest request);

    default java.math.BigDecimal scaleHalfUp(java.math.BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}