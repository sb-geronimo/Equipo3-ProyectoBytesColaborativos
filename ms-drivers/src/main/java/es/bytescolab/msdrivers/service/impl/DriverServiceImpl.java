package es.bytescolab.msdrivers.service.impl;

import es.bytescolab.msdrivers.dto.request.DriverCreateRequest;
import es.bytescolab.msdrivers.dto.request.DriverUpdateRequest;
import es.bytescolab.msdrivers.dto.response.DriverDetailResponse;
import es.bytescolab.msdrivers.dto.response.DriverSummaryResponse;
import es.bytescolab.msdrivers.dto.response.PageResponse;
import es.bytescolab.msdrivers.entity.DriverEntity;
import es.bytescolab.msdrivers.enums.DriverStatus;
import es.bytescolab.msdrivers.mapper.DriverMapper;
import es.bytescolab.msdrivers.repository.DriverRepository;
import es.bytescolab.msdrivers.service.DriverService;
import es.bytescolab.msdrivers.specification.DriverSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository repository;
    private final DriverMapper mapper;

    @Override
    public PageResponse<DriverSummaryResponse> findAll(DriverStatus status, Integer licenseExpiringInDays, Pageable pageable) {
        var specification = Specification
                .where(DriverSpecification.hasStatus(status))
                .and(DriverSpecification.hasLicenseExpiringInDays(licenseExpiringInDays));

        Page<DriverEntity> drivers = repository.findAll(specification, pageable);
        return PageResponse.from(drivers, mapper::toSummaryResponse);
    }

    @Override
    public DriverDetailResponse findById(UUID driverId) {
        DriverEntity driver = repository
                .findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        return mapper.toDetailResponse(driver);
    }

    @Override
    public DriverDetailResponse create(DriverCreateRequest request) {
        if (repository.existsByLicenseNumber(request.licenseNumber())) {
            throw new RuntimeException("Driver with license number already exists: " + request.licenseNumber());
        }

        DriverEntity driver = mapper.toEntity(request);
        driver = repository.save(driver);

        return mapper.toDetailResponse(driver);
    }

    @Override
    public DriverDetailResponse update(UUID driverId, DriverUpdateRequest request) {
        DriverEntity driver = repository
                .findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + driverId));

        mapper.updateEntityFromRequest(request, driver);

        driver = repository.save(driver);

        return mapper.toDetailResponse(driver);
    }
}
