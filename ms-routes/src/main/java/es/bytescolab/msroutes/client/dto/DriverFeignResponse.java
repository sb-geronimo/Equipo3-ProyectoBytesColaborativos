package es.bytescolab.msroutes.client.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DriverFeignResponse(
        UUID id,
        String status,
        String licenseCategory,
        LocalDate licenseExpiresAt
) {
}