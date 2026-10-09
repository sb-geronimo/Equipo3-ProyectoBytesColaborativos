package es.bytescolab.msmaintenance.controller;

import es.bytescolab.msmaintenance.dto.request.SchedulePlanRequest;
import es.bytescolab.msmaintenance.dto.request.CompleteOrderRequest;
import es.bytescolab.msmaintenance.dto.response.*;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import es.bytescolab.msmaintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping("/plans")
    public ResponseEntity<PlanDetailsResponse> plans(
            @Valid @RequestBody SchedulePlanRequest request
    ) {
        PlanDetailsResponse response = maintenanceService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanDetailsResponse>> getPlans(
            @RequestParam(required = false) UUID vehicle,
            @RequestParam(required = false) MaintenanceType type,
            @RequestParam(required = false) Boolean active
    ) {
        List<PlanDetailsResponse> response = maintenanceService.listPlans(vehicle, type, active);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<PageResponse<OrderSummaryResponse>> listOrders(
            @RequestParam(required = false) UUID vehicle,
            @RequestParam(required = false) MaintenanceOrderStatus status,
            @RequestParam(required = false) MaintenanceType type,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate dueBefore,
            @PositiveOrZero @RequestParam(defaultValue = "0") Integer page,
            @Max(100) @RequestParam(defaultValue = "20") Integer size
    ) {
        PageResponse<OrderSummaryResponse> response =
                maintenanceService.findOrders(vehicle, status, type, dueBefore, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders/{orderId}/start")
    public ResponseEntity<StartedOrderResponse> startOrder(@PathVariable UUID orderId) {
        StartedOrderResponse response = maintenanceService.startOrder(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders/{orderId}/complete")
    public ResponseEntity<?> completeOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CompleteOrderRequest request
    ) {
        CompletedOrderResponse response = maintenanceService.completeOrder(orderId, request);
        return ResponseEntity.ok(response);
    }
}
