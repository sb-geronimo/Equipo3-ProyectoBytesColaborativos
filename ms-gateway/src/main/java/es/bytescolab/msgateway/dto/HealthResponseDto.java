package es.bytescolab.msgateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record HealthResponseDto(
        @JsonProperty("gateway")
        String gateway,

        @JsonProperty("timestamp")
        String timestamp,

        @JsonProperty("services")
        Map<String, String> services
) {
}
