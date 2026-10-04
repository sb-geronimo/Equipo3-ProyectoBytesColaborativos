package es.bytescolab.msvehicles.controller;

import es.bytescolab.msvehicles.dto.request.CreateVehicleRequest;
import es.bytescolab.msvehicles.dto.request.StatusVehicleRequest;
import es.bytescolab.msvehicles.dto.request.UpdateVehicleRequest;
import es.bytescolab.msvehicles.dto.response.PageResponse;
import es.bytescolab.msvehicles.dto.response.VehicleDetailsResponse;
import es.bytescolab.msvehicles.dto.response.VehicleSummaryResponse;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleDetailsResponse> createVehicle(
            @Valid @RequestBody CreateVehicleRequest vehicleRequest
    ) {
        VehicleDetailsResponse response = vehicleService.createVehicle(vehicleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<VehicleSummaryResponse>> findVehicles(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) VehicleType type,
            @RequestParam(required = false) String plate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        PageResponse<VehicleSummaryResponse> response = vehicleService.findVehicles(pageable, status, type, plate);
        return ResponseEntity
                .ok(response);
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<?> findVehicleById(@PathVariable UUID vehicleId) {
        VehicleDetailsResponse response = vehicleService.findVehicleById(vehicleId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleSummaryResponse> updateVehicle(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest vehicleRequest
    ) {
        VehicleSummaryResponse response = vehicleService.updateVehicle(vehicleId, vehicleRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{vehicleId}/status")
    public ResponseEntity<VehicleSummaryResponse> updateVehicle(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody StatusVehicleRequest request
    ) {
        VehicleSummaryResponse response = vehicleService.updateStatusVehicle(vehicleId, request);
        return ResponseEntity.ok(response);
    }
}
