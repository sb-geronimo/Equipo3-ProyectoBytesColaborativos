package es.bytescolab.msdrivers.service.impl;

import es.bytescolab.msdrivers.dto.request.DriverCreateRequest;
import es.bytescolab.msdrivers.dto.request.DriverUpdateRequest;
import es.bytescolab.msdrivers.dto.response.DriverDetailResponse;
import es.bytescolab.msdrivers.dto.response.DriverSummaryResponse;
import es.bytescolab.msdrivers.dto.response.PageResponse;
import es.bytescolab.msdrivers.entity.DriverEntity;
import es.bytescolab.msdrivers.enums.DriverStatus;
import es.bytescolab.msdrivers.enums.LicenseCategory;
import es.bytescolab.msdrivers.exception.DriverNotFoundException;
import es.bytescolab.msdrivers.exception.LicenseNumberAlreadyExistsException;
import es.bytescolab.msdrivers.mapper.DriverMapper;
import es.bytescolab.msdrivers.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

    @Mock
    private DriverRepository repository;

    @Mock
    private DriverMapper mapper;

    @InjectMocks
    private DriverServiceImpl driverService;

    private UUID driverId;
    private DriverEntity driver;
    private DriverCreateRequest createRequest;
    private DriverUpdateRequest updateRequest;
    private DriverDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        driverId = UUID.fromString("3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01");
        LocalDate expiry = LocalDate.now().plusYears(3);

        driver = DriverEntity.builder()
                .id(driverId)
                .fullName("Sara Ibáñez")
                .email("sara.ibanez@fleetcontrol.com")
                .phone("+34 622 456 789")
                .licenseNumber("B-5120377")
                .licenseCategory(LicenseCategory.B)
                .licenseExpiresAt(expiry)
                .status(DriverStatus.ACTIVE)
                .createdAt(Instant.parse("2026-10-05T10:30:00Z"))
                .updatedAt(Instant.parse("2026-10-05T10:30:00Z"))
                .isNew(false)
                .build();

        createRequest = new DriverCreateRequest(
                "Sara Ibáñez",
                "sara.ibanez@fleetcontrol.com",
                "+34 622 456 789",
                "B-5120377",
                LicenseCategory.B,
                expiry
        );

        updateRequest = new DriverUpdateRequest(
                "Sara Ibáñez Renovada",
                "sara.ibanez@fleetcontrol.com",
                "+34 622 456 789",
                "B-5120377",
                LicenseCategory.B,
                expiry,
                DriverStatus.ON_LEAVE
        );

        detailResponse = new DriverDetailResponse(
                driverId,
                "Sara Ibáñez",
                "sara.ibanez@fleetcontrol.com",
                "+34 622 456 789",
                "B-5120377",
                LicenseCategory.B,
                expiry,
                DriverStatus.ACTIVE,
                Instant.parse("2026-10-05T10:30:00Z"),
                Instant.parse("2026-10-05T10:30:00Z")
        );
    }

    @Test
    @DisplayName("create should return DriverDetailResponse with persisted entity when license number is unique")
    void create_should_returnDetailResponse_when_licenseNumberIsUnique() {
        when(repository.existsByLicenseNumber(createRequest.licenseNumber())).thenReturn(false);
        when(mapper.toEntity(createRequest)).thenReturn(driver);
        when(repository.save(any(DriverEntity.class))).thenReturn(driver);
        when(mapper.toDetailResponse(driver)).thenReturn(detailResponse);

        DriverDetailResponse actual = driverService.create(createRequest);

        assertThat(actual).isEqualTo(detailResponse);

        ArgumentCaptor<DriverEntity> savedCaptor = ArgumentCaptor.forClass(DriverEntity.class);
        verify(repository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).isSameAs(driver);
        verify(mapper).toDetailResponse(driver);
    }

    @Test
    @DisplayName("create should throw LicenseNumberAlreadyExistsException when license number already exists")
    void create_should_throwDuplicateException_when_licenseNumberExists() {
        when(repository.existsByLicenseNumber(createRequest.licenseNumber())).thenReturn(true);

        assertThatThrownBy(() -> driverService.create(createRequest))
                .isInstanceOf(LicenseNumberAlreadyExistsException.class)
                .hasMessageContaining("licencia");

        verify(repository, never()).save(any(DriverEntity.class));
        verify(mapper, never()).toEntity(any(DriverCreateRequest.class));
    }

    @Test
    @DisplayName("findAll should return page mapped to summary DTOs when filtered by status")
    void findAll_should_returnSummaryPage_when_statusFilterApplied() {
        Pageable pageable = PageRequest.of(0, 20);
        DriverEntity second = DriverEntity.builder()
                .id(UUID.fromString("4c1d9e21-8b5c-4f66-c1b1-6eaf3d8e2b12"))
                .fullName("Laura Fernández")
                .email("laura.fernandez@fleetcontrol.com")
                .phone("+34 600 123 456")
                .licenseNumber("B-4471923")
                .licenseCategory(LicenseCategory.B)
                .licenseExpiresAt(LocalDate.now().plusYears(2))
                .status(DriverStatus.ACTIVE)
                .isNew(false)
                .build();
        DriverSummaryResponse firstSummary = new DriverSummaryResponse(
                driver.getId(),
                driver.getFullName(),
                driver.getEmail(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getLicenseCategory(),
                driver.getLicenseExpiresAt(),
                driver.getStatus()
        );
        DriverSummaryResponse secondSummary = new DriverSummaryResponse(
                second.getId(),
                second.getFullName(),
                second.getEmail(),
                second.getPhone(),
                second.getLicenseNumber(),
                second.getLicenseCategory(),
                second.getLicenseExpiresAt(),
                second.getStatus()
        );
        Page<DriverEntity> entityPage = new PageImpl<>(List.of(driver, second), pageable, 2);

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
        when(mapper.toSummaryResponse(driver)).thenReturn(firstSummary);
        when(mapper.toSummaryResponse(second)).thenReturn(secondSummary);

        PageResponse<DriverSummaryResponse> response = driverService.findAll(DriverStatus.ACTIVE, null, pageable);

        assertThat(response.content()).containsExactly(firstSummary, secondSummary);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("findAll should return page mapped to summary DTOs when filtered by licenseExpiringInDays")
    void findAll_should_returnSummaryPage_when_licenseExpiringFilterApplied() {
        Pageable pageable = PageRequest.of(0, 10);
        DriverSummaryResponse summary = new DriverSummaryResponse(
                driver.getId(),
                driver.getFullName(),
                driver.getEmail(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getLicenseCategory(),
                driver.getLicenseExpiresAt(),
                driver.getStatus()
        );
        Page<DriverEntity> entityPage = new PageImpl<>(List.of(driver), pageable, 1);

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
        when(mapper.toSummaryResponse(driver)).thenReturn(summary);

        PageResponse<DriverSummaryResponse> response = driverService.findAll(null, 30, pageable);

        assertThat(response.content()).containsExactly(summary);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("update should throw DriverNotFoundException when no driver matches the id")
    void update_should_throwNotFound_when_driverMissing() {
        when(repository.findById(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.update(driverId, updateRequest))
                .isInstanceOf(DriverNotFoundException.class)
                .hasMessageContaining("ID");

        verify(repository, never()).save(any(DriverEntity.class));
    }

    @Test
    @DisplayName("update should persist and return detail response when license number is unchanged")
    void update_should_returnDetailResponse_when_licenseNumberUnchanged() {
        when(repository.findById(driverId)).thenReturn(Optional.of(driver));
        when(repository.save(driver)).thenReturn(driver);
        when(mapper.toDetailResponse(driver)).thenReturn(detailResponse);

        DriverDetailResponse actual = driverService.update(driverId, updateRequest);

        assertThat(actual).isEqualTo(detailResponse);
        verify(repository, never()).existsByLicenseNumber(any());
        verify(mapper).updateEntityFromRequest(updateRequest, driver);
        verify(repository).save(driver);
    }

    @Test
    @DisplayName("update should throw LicenseNumberAlreadyExistsException when new license number already exists")
    void update_should_throwDuplicateException_when_newLicenseNumberExists() {
        DriverUpdateRequest changedLicenseRequest = new DriverUpdateRequest(
                "Sara Ibáñez Renovada",
                "sara.ibanez@fleetcontrol.com",
                "+34 622 456 789",
                "B-9999999",
                LicenseCategory.B,
                LocalDate.now().plusYears(3),
                DriverStatus.ON_LEAVE
        );

        when(repository.findById(driverId)).thenReturn(Optional.of(driver));
        when(repository.existsByLicenseNumber("B-9999999")).thenReturn(true);

        assertThatThrownBy(() -> driverService.update(driverId, changedLicenseRequest))
                .isInstanceOf(LicenseNumberAlreadyExistsException.class);

        verify(repository, never()).save(any(DriverEntity.class));
        verify(mapper, never()).updateEntityFromRequest(any(), any());
    }
}
