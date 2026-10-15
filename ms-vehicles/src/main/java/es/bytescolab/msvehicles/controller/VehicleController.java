package es.bytescolab.msvehicles.controller;

import es.bytescolab.msvehicles.dto.request.CreateVehicleRequest;
import es.bytescolab.msvehicles.dto.request.StatusVehicleRequest;
import es.bytescolab.msvehicles.dto.request.UpdateVehicleRequest;
import es.bytescolab.msvehicles.dto.response.FleetSummaryResponse;
import es.bytescolab.msvehicles.dto.response.PageResponse;
import es.bytescolab.msvehicles.dto.response.VehicleDetailsResponse;
import es.bytescolab.msvehicles.dto.response.VehicleSummaryResponse;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Vehicles", description = "Gestión del inventario y resumen agregado de la flota")
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "Resumen agregado de la flota",
            description = "Devuelve el total de vehículos y el desglose por estado y por tipo en una sola "
                    + "respuesta agregada. Lo consume ms-dashboard."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen de la flota",
                    content = @Content(schema = @Schema(implementation = FleetSummaryResponse.class)))
    })
    @GetMapping("/summary")
    public ResponseEntity<FleetSummaryResponse> getFleetSummary() {
        FleetSummaryResponse response = vehicleService.getFleetSummary();
        return ResponseEntity.ok(response);
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
            @PathVariable UUID vehicleId, @Valid @RequestBody StatusVehicleRequest request
    ) {
        VehicleSummaryResponse response = vehicleService.updateStatusVehicle(vehicleId, request);
        return ResponseEntity.ok(response);
    }
}
