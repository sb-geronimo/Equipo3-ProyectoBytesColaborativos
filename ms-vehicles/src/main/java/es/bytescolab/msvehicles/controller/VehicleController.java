package es.bytescolab.msvehicles.controller;

import es.bytescolab.msvehicles.dto.request.CreateVehicleRequest;
import es.bytescolab.msvehicles.dto.request.StatusVehicleRequest;
import es.bytescolab.msvehicles.dto.request.UpdateVehicleRequest;
import es.bytescolab.msvehicles.dto.response.ErrorResponse;
import es.bytescolab.msvehicles.dto.response.FleetSummaryResponse;
import es.bytescolab.msvehicles.dto.response.PageResponse;
import es.bytescolab.msvehicles.dto.response.VehicleDetailsResponse;
import es.bytescolab.msvehicles.dto.response.VehicleSummaryResponse;
import es.bytescolab.msvehicles.enums.VehicleStatus;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(
            summary = "Registrar un vehículo",
            description = "Da de alta un vehículo con odómetro inicial. Nace en estado AVAILABLE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehículo creado",
                    content = @Content(schema = @Schema(implementation = VehicleDetailsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La placa ya existe (VEHICLE_ALREADY_EXISTS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<VehicleDetailsResponse> createVehicle(
            @Valid @RequestBody CreateVehicleRequest vehicleRequest
    ) {
        VehicleDetailsResponse response = vehicleService.createVehicle(vehicleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Listar vehículos paginados",
            description = "Devuelve la página de vehículos filtrada por estado, tipo y/o placa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de vehículos",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<VehicleSummaryResponse>> findVehicles(
            @Parameter(description = "Filtro por estado del vehículo", example = "AVAILABLE")
            @RequestParam(required = false) VehicleStatus status,

            @Parameter(description = "Filtro por tipo de vehículo", example = "VAN")
            @RequestParam(required = false) VehicleType type,

            @Parameter(description = "Filtro por placa (coincidencia parcial)", example = "4821-KDF")
            @RequestParam(required = false) String plate,

            @Parameter(description = "Número de página (0 en adelante)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "Tamaño de página (por defecto 20, máximo 100)", example = "20")
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

    @Operation(
            summary = "Consultar el detalle de un vehículo",
            description = "Devuelve los datos completos del vehículo, incluidos odómetro y estado actuales."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle del vehículo",
                    content = @Content(schema = @Schema(implementation = VehicleDetailsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo inexistente (VEHICLE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{vehicleId}")
    public ResponseEntity<?> findVehicleById(
            @Parameter(description = "UUID del vehículo")
            @PathVariable UUID vehicleId) {
        VehicleDetailsResponse response = vehicleService.findVehicleById(vehicleId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Actualizar los datos descriptivos de un vehículo",
            description = "Actualiza marca, modelo, año, tipo, combustible y capacidad del tanque. "
                    + "No modifica el estado ni el odómetro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehículo actualizado",
                    content = @Content(schema = @Schema(implementation = VehicleSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo inexistente (VEHICLE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleSummaryResponse> updateVehicle(
            @Parameter(description = "UUID del vehículo")
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest vehicleRequest
    ) {
        VehicleSummaryResponse response = vehicleService.updateVehicle(vehicleId, vehicleRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cambiar el estado y el odómetro de un vehículo",
            description = "Aplica una transición de estado validada y, opcionalmente, actualiza el odómetro "
                    + "(solo permite aumentarlo)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado",
                    content = @Content(schema = @Schema(implementation = VehicleSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo inexistente (VEHICLE_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Transición no permitida u odómetro inválido "
                    + "(INVALID_STATUS_TRANSITION / INVALID_ODOMETER)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{vehicleId}/status")
    public ResponseEntity<VehicleSummaryResponse> updateVehicle(
            @Parameter(description = "UUID del vehículo")
            @PathVariable UUID vehicleId, @Valid @RequestBody StatusVehicleRequest request
    ) {
        VehicleSummaryResponse response = vehicleService.updateStatusVehicle(vehicleId, request);
        return ResponseEntity.ok(response);
    }
}
