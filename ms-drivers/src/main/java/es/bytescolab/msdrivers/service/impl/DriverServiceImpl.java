package es.bytescolab.msdrivers.service.impl;

import es.bytescolab.msdrivers.dto.request.DriverCreateRequest;
import es.bytescolab.msdrivers.dto.request.DriverUpdateRequest;
import es.bytescolab.msdrivers.dto.response.DriverDetailResponse;
import es.bytescolab.msdrivers.dto.response.DriverSummaryResponse;
import es.bytescolab.msdrivers.dto.response.PageResponse;
import es.bytescolab.msdrivers.entity.DriverEntity;
import es.bytescolab.msdrivers.enums.DriverStatus;
import es.bytescolab.msdrivers.exception.DriverNotFoundException;
import es.bytescolab.msdrivers.exception.LicenseNumberAlreadyExistsException;
import es.bytescolab.msdrivers.mapper.DriverMapper;
import es.bytescolab.msdrivers.repository.DriverRepository;
import es.bytescolab.msdrivers.service.DriverService;
import es.bytescolab.msdrivers.specification.DriverSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
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
        DriverEntity driver = this.findDriverById(driverId);
        return mapper.toDetailResponse(driver);
    }

    @Override
    public DriverDetailResponse create(DriverCreateRequest request) {
        this.ensureLicenseNumberIsUnique(request.licenseNumber());

        log.debug("Creating new driver with license number: {}", request.licenseNumber());
        DriverEntity driver = mapper.toEntity(request);
        log.debug("Saving new driver to the database: {}", driver);
        driver = repository.save(driver);

        return mapper.toDetailResponse(driver);
    }

    @Override
    public DriverDetailResponse update(UUID driverId, DriverUpdateRequest request) {
        DriverEntity driver = this.findDriverById(driverId);

        if (!driver.getLicenseNumber().equals(request.licenseNumber())) {
            this.ensureLicenseNumberIsUnique(request.licenseNumber());
        }

        mapper.updateEntityFromRequest(request, driver);

        driver = repository.save(driver);

        return mapper.toDetailResponse(driver);
    }

    private DriverEntity findDriverById(UUID id) {
        return repository
                .findById(id)
                .orElseThrow(DriverNotFoundException::new);
    }

    private void ensureLicenseNumberIsUnique(String licenseNumber) {
        if (repository.existsByLicenseNumber(licenseNumber)) {
            throw new LicenseNumberAlreadyExistsException();
        }
    }
}
