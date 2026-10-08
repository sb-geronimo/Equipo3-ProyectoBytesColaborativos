package es.bytescolab.msroutes.client.dto;

import java.util.UUID;

public record VehicleFeignResponse(
        UUID id,
        String status,
        String type
) {
}