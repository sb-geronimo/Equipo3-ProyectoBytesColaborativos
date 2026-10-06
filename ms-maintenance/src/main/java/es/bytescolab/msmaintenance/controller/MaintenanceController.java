package es.bytescolab.msmaintenance.controller;

import es.bytescolab.msmaintenance.dto.request.CreatePlanRequest;
import es.bytescolab.msmaintenance.dto.response.CreatePlanResponse;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import es.bytescolab.msmaintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping("/plans")
    public ResponseEntity<CreatePlanResponse> plans(
            @Valid @RequestBody CreatePlanRequest request
    ) {
        CreatePlanResponse response = maintenanceService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<CreatePlanResponse>> getPlans(
            @RequestParam(required = false) UUID vehicle,
            @RequestParam(required = false) MaintenanceType type,
            @RequestParam(required = false) Boolean active
    ) {
        List<CreatePlanResponse> response = maintenanceService.listPlans(vehicle, type, active);
        return ResponseEntity.ok(response);
    }
}
