package es.bytescolab.msdrivers.service;

import es.bytescolab.msdrivers.dto.request.DriverCreateRequest;
import es.bytescolab.msdrivers.dto.request.DriverUpdateRequest;
import es.bytescolab.msdrivers.dto.response.DriverDetailResponse;
import es.bytescolab.msdrivers.dto.response.DriverSummaryResponse;
import es.bytescolab.msdrivers.dto.response.PageResponse;
import es.bytescolab.msdrivers.enums.DriverStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DriverService {

    PageResponse<DriverSummaryResponse> findAll(
            DriverStatus status,
            Integer licenseExpiringInDays,
            Pageable pageable
    );

    DriverDetailResponse findById(UUID driverId);

    DriverDetailResponse create(DriverCreateRequest request);

    DriverDetailResponse update(UUID driverId, DriverUpdateRequest request);
}