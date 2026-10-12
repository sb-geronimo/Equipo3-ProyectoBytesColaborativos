package es.bytescolab.msalerts.mapper;

import es.bytescolab.msalerts.dto.request.UpdateAlertRuleRequest;
import es.bytescolab.msalerts.dto.response.AlertRuleResponse;
import es.bytescolab.msalerts.entity.AlertRuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertRuleMapper {

    @Mapping(target = "params", expression = "java(toMap(entity.getParams()))")
    AlertRuleResponse toResponse(AlertRuleEntity entity);

    @Mapping(target = "type", ignore = true)
    @Mapping(target = "params", expression = "java(toJsonNode(request.params()))")
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget AlertRuleEntity entity, UpdateAlertRuleRequest request);

    default JsonNode toJsonNode(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new ObjectMapper().createObjectNode();
        }
        return new ObjectMapper().valueToTree(params);
    }

    default Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        return new ObjectMapper().convertValue(node, new TypeReference<Map<String, Object>>() {
        });
    }
}