package es.bytescolab.msalerts.mapper;

import es.bytescolab.msalerts.dto.response.AlertResponse;
import es.bytescolab.msalerts.entity.AlertEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertMapper {

    AlertResponse toResponse(AlertEntity entity);
}