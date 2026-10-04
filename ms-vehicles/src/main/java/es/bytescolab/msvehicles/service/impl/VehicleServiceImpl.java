package es.bytescolab.msvehicles.service.impl;

import es.bytescolab.msvehicles.dto.request.CreateVehicleRequest;
import es.bytescolab.msvehicles.dto.request.StatusVehicleRequest;
import es.bytescolab.msvehicles.dto.request.UpdateVehicleRequest;
import es.bytescolab.msvehicles.dto.response.PageResponse;
import es.bytescolab.msvehicles.dto.response.VehicleDetailsResponse;
import es.bytescolab.msvehicles.dto.response.VehicleSummaryResponse;
import es.bytescolab.msvehicles.entity.Vehicle;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.exception.*;
import es.bytescolab.msvehicles.mapper.VehicleMapper;
import es.bytescolab.msvehicles.repository.VehicleRepository;
import es.bytescolab.msvehicles.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    @Transactional(readOnly = true)
    public VehicleDetailsResponse findVehicleById(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFound("No existe un vehículo con el ID proporcionado"));
        return vehicleMapper.toDetailsResponse(vehicle);
    }

    @Override
    @Transactional
    public VehicleSummaryResponse updateStatusVehicle(UUID vehicleId, StatusVehicleRequest request) {
        Vehicle vehicleToUpdate = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFound("Vehículo no encontrado"));

        if ((vehicleToUpdate.getStatus() == VehicleStatus.OUT_OF_SERVICE) && !isAdmin()) {
            throw new InsufficientAuthorityException("Operacion no permitida, autoridad insuficiente");
        }

        if (!vehicleToUpdate.getStatus().canTransitionTo(request.status())) {
            throw new InvalidStatusTransitionException("La transición no está permitida");
        }

        if (request.odometerKm() != null) {

            if (vehicleToUpdate.getOdometerKm() > request.odometerKm()) {
                throw new InvalidOdometerException("El valor del odómetro no puede ser menor que el actual");
            }
            vehicleToUpdate.setOdometerKm(request.odometerKm());
        }
        vehicleToUpdate.setStatus(request.status());


        Vehicle vehicleUpdated = vehicleRepository.save(vehicleToUpdate);
        return vehicleMapper.toSummaryResponse(vehicleUpdated);
    }

    @Override
    @Transactional
    public VehicleSummaryResponse updateVehicle(UUID vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicleToUpdate = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFound("Vehículo no encontrado"));
        vehicleToUpdate.setMake(request.make());
        vehicleToUpdate.setModel(request.model());
        vehicleToUpdate.setYear(request.year());
        vehicleToUpdate.setType(request.type());
        vehicleToUpdate.setFuelType(request.fuelType());
        vehicleToUpdate.setTankCapacityL(request.tankCapacityL());

        Vehicle vehicleUpdated = vehicleRepository.save(vehicleToUpdate);
        return vehicleMapper.toSummaryResponse(vehicleUpdated);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VehicleSummaryResponse> findVehicles(
            Pageable pageable, VehicleStatus status, VehicleType type, String plate
    ) {

        Specification<Vehicle> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }

        if (type != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("type"), type));
        }

        if (plate != null && !plate.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("plate")),
                            "%" + plate.toLowerCase() + "%"
                    ));
        }

        Page<Vehicle> vehicles = vehicleRepository.findAll(spec, pageable);
        Page<VehicleSummaryResponse> response = vehicles.map(vehicleMapper::toSummaryResponse);

        return new PageResponse<VehicleSummaryResponse>(
                response.getContent(),
                response.getNumber(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages()
        );
    }

    @Override
    @Transactional
    public VehicleDetailsResponse createVehicle(CreateVehicleRequest request) {
        Vehicle vehicle = vehicleMapper.toEntity(request);

        try {
            Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
            return vehicleMapper.toDetailsResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new VehicleAlreadyExists(
                    "Ya existe un vehículo con la placa: " + request.plate()
            );
        }
    }

    private boolean isAdmin() {
        return Objects.requireNonNull(SecurityContextHolder.getContext()
                        .getAuthentication())
                .getAuthorities()
                .stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
    }
}
