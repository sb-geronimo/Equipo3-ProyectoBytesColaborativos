package es.bytescolab.msroutes.dto.request;

import es.bytescolab.msroutes.enums.RouteStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.UUID;

public record RouteFilterRequest(
        UUID vehicle,
        UUID driver,
        RouteStatus status,
        LocalDate from,
        LocalDate to,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public RouteFilterRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
    }
}
