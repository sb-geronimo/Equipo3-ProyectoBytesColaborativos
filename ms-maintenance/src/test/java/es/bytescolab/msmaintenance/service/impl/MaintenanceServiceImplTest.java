package es.bytescolab.msmaintenance.service.impl;

import es.bytescolab.msmaintenance.client.VehicleClient;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusChangeRequest;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusSnapshot;
import es.bytescolab.msmaintenance.client.vehicle.enums.VehicleStatus;
import es.bytescolab.msmaintenance.dto.request.CompleteOrderRequest;
import es.bytescolab.msmaintenance.dto.request.SchedulePlanRequest;
import es.bytescolab.msmaintenance.dto.response.CompletedOrderResponse;
import es.bytescolab.msmaintenance.dto.response.PlanDetailsResponse;
import es.bytescolab.msmaintenance.dto.response.StartedOrderResponse;
import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import es.bytescolab.msmaintenance.exception.InvalidOdometerException;
import es.bytescolab.msmaintenance.exception.InvalidOrderState;
import es.bytescolab.msmaintenance.exception.PlanAreadyExists;
import es.bytescolab.msmaintenance.exception.VehicleNotAvailable;
import es.bytescolab.msmaintenance.mapper.MaintenanceMapper;
import es.bytescolab.msmaintenance.repository.MaintenanceOrderRepository;
import es.bytescolab.msmaintenance.repository.MaintenancePlanRespository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceImplTest {

    @Mock
    private MaintenancePlanRespository maintenancePlanRespository;

    @Mock
    private MaintenanceOrderRepository maintenanceOrderRepository;

    @Mock
    private MaintenanceMapper maintenanceMapper;

    @Mock
    private VehicleClient vehicleClient;

    @InjectMocks
    private MaintenanceServiceImpl service;

    private UUID vehicleId;
    private UUID orderId;
    private UUID planId;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        planId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    }

    @Nested
    class CreatePlan {

        @Test
        void createPlan_should_returnPlanDetailsResponse_when_noActivePlanExists() {
            SchedulePlanRequest request = new SchedulePlanRequest(
                    vehicleId, MaintenanceType.OIL_CHANGE, 10000, 180,
                    LocalDate.of(2026, 1, 1), 20000
            );
            MaintenancePlan mapped = basePlan();
            mapped.setLastDoneAt(LocalDate.of(2026, 1, 1));
            mapped.setLastDoneKm(20000);
            mapped.setNextDueAt(LocalDate.of(2026, 1, 1).plusDays(180));
            mapped.setNextDueKm(30000);
            PlanDetailsResponse expected = new PlanDetailsResponse(
                    planId, vehicleId, MaintenanceType.OIL_CHANGE,
                    10000, 180, LocalDate.of(2026, 1, 1), 20000,
                    LocalDate.of(2026, 7, 1), 30000, true
            );

            when(maintenanceMapper.toMaintenancePlan(request)).thenReturn(mapped);
            when(maintenancePlanRespository.existsByVehicleIdAndTypeAndActiveTrue(vehicleId, MaintenanceType.OIL_CHANGE))
                    .thenReturn(false);
            when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicleSnapshot(50000));
            when(maintenancePlanRespository.save(mapped)).thenReturn(mapped);
            when(maintenanceMapper.toPlanDetailsResponse(mapped)).thenReturn(expected);

            PlanDetailsResponse result = service.createPlan(request);

            assertThat(result).isSameAs(expected);
            assertThat(mapped.getActive()).isTrue();
            verify(maintenancePlanRespository).save(mapped);
            verify(maintenanceMapper).toPlanDetailsResponse(mapped);
        }

        @Test
        void createPlan_should_throwPlanAlreadyExists_when_activePlanForSameVehicleAndTypeExists() {
            SchedulePlanRequest request = new SchedulePlanRequest(
                    vehicleId, MaintenanceType.OIL_CHANGE, 10000, 180,
                    LocalDate.of(2026, 1, 1), 20000
            );
            when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicleSnapshot(50000));
            when(maintenancePlanRespository.existsByVehicleIdAndTypeAndActiveTrue(vehicleId, MaintenanceType.OIL_CHANGE))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createPlan(request))
                    .isInstanceOf(PlanAreadyExists.class)
                    .hasMessageContaining("plan activo");
            verify(maintenancePlanRespository, never()).save(any(MaintenancePlan.class));
        }

        @Test
        void createPlan_should_defaultLastDoneFieldsFromVehicle_when_requestOmitted() {
            SchedulePlanRequest request = new SchedulePlanRequest(
                    vehicleId, MaintenanceType.BRAKE_CHECK, 5000, 90,
                    null, null
            );
            MaintenancePlan mapped = basePlan();
            mapped.setType(MaintenanceType.BRAKE_CHECK);
            when(maintenanceMapper.toMaintenancePlan(request)).thenReturn(mapped);
            when(maintenancePlanRespository.existsByVehicleIdAndTypeAndActiveTrue(vehicleId, MaintenanceType.BRAKE_CHECK))
                    .thenReturn(false);
            when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicleSnapshot(75000));
            when(maintenancePlanRespository.save(mapped)).thenReturn(mapped);
            when(maintenanceMapper.toPlanDetailsResponse(mapped)).thenReturn(anyPlanDetailsResponse());

            service.createPlan(request);

            assertThat(mapped.getLastDoneKm()).isEqualTo(75000);
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            assertThat(mapped.getLastDoneAt()).isEqualTo(today);
            assertThat(mapped.getNextDueKm()).isEqualTo(80000);
            assertThat(mapped.getNextDueAt()).isEqualTo(today.plusDays(90));
        }
    }

    @Nested
    class StartOrder {

        @Test
        void startOrder_should_transitionToInProgressAndUpdateVehicleToInMaintenance_when_statusPendingAndVehicleAvailable() {
            MaintenanceOrder order = baseOrder();
            order.setStatus(MaintenanceOrderStatus.PENDING);
            when(maintenanceOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicleSnapshot(VehicleStatus.AVAILABLE, 50000));
            when(maintenanceOrderRepository.save(order)).thenReturn(order);

            StartedOrderResponse result = service.startOrder(orderId);

            assertThat(result.status()).isEqualTo(MaintenanceOrderStatus.IN_PROGRESS);
            assertThat(result.startedAt()).isNotNull();
            assertThat(order.getStatus()).isEqualTo(MaintenanceOrderStatus.IN_PROGRESS);
            ArgumentCaptor<StatusChangeRequest> captor = ArgumentCaptor.forClass(StatusChangeRequest.class);
            verify(vehicleClient).updateVehicle(any(UUID.class), captor.capture());
            assertThat(captor.getValue().status()).isEqualTo(VehicleStatus.IN_MAINTENANCE);
        }

        @Test
        void startOrder_should_throwInvalidOrderState_when_statusIsAlreadyInProgress() {
            MaintenanceOrder order = baseOrder();
            order.setStatus(MaintenanceOrderStatus.IN_PROGRESS);
            when(maintenanceOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.startOrder(orderId))
                    .isInstanceOf(InvalidOrderState.class)
                    .hasMessageContaining("pendiente");
            verify(vehicleClient, never()).getVehicle(any(UUID.class));
        }

        @Test
        void startOrder_should_throwInvalidOrderState_when_statusIsAlreadyCompleted() {
            MaintenanceOrder order = baseOrder();
            order.setStatus(MaintenanceOrderStatus.COMPLETED);
            when(maintenanceOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.startOrder(orderId))
                    .isInstanceOf(InvalidOrderState.class);
            verify(vehicleClient, never()).getVehicle(any(UUID.class));
        }

        @Test
        void startOrder_should_throwVehicleNotAvailable_when_vehicleStatusIsNotAvailable() {
            MaintenanceOrder order = baseOrder();
            order.setStatus(MaintenanceOrderStatus.PENDING);
            when(maintenanceOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicleSnapshot(VehicleStatus.IN_USE, 50000));

            assertThatThrownBy(() -> service.startOrder(orderId))
                    .isInstanceOf(VehicleNotAvailable.class)
                    .hasMessageContaining("disponible");
            verify(vehicleClient, never()).updateVehicle(any(UUID.class), any(StatusChangeRequest.class));
            verify(maintenanceOrderRepository, never()).save(any(MaintenanceOrder.class));
        }
    }

    @Nested
    class CompleteOrder {

        @Test
        void completeOrder_should_transitionToCompletedAndRecalculatePlan_when_statusInProgressAndOdometerValid() {
            MaintenanceOrder order = baseOrder();
            order.setStatus(MaintenanceOrderStatus.IN_PROGRESS);
            MaintenancePlan plan = basePlan();
            plan.setIntervalDays(180);
            plan.setIntervalKm(10000);
            plan.setLastDoneAt(LocalDate.of(2026, 1, 1));
            plan.setLastDoneKm(50000);
            plan.setNextDueAt(LocalDate.of(2026, 7, 1));
            plan.setNextDueKm(60000);

            CompleteOrderRequest request = new CompleteOrderRequest(
                    new BigDecimal("250.00"), 52500, "Taller oficial", "Cambio de aceite"
            );

            when(maintenanceOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicleSnapshot(VehicleStatus.IN_MAINTENANCE, 50000));
            when(maintenancePlanRespository.findById(planId)).thenReturn(Optional.of(plan));
            when(maintenancePlanRespository.save(any(MaintenancePlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(maintenanceOrderRepository.save(any(MaintenanceOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CompletedOrderResponse result = service.completeOrder(orderId, request);

            assertThat(order.getStatus()).isEqualTo(MaintenanceOrderStatus.COMPLETED);
            assertThat(order.getCompletedAt()).isNotNull();
            assertThat(order.getOdometerKm()).isEqualTo(52500);
            assertThat(result.status()).isEqualTo(MaintenanceOrderStatus.COMPLETED);
            assertThat(result.odometerKm()).isEqualTo(52500);
            assertThat(result.plan()).isNotNull();

            ArgumentCaptor<MaintenancePlan> planCaptor = ArgumentCaptor.forClass(MaintenancePlan.class);
            verify(maintenancePlanRespository).save(planCaptor.capture());
            MaintenancePlan savedPlan = planCaptor.getValue();
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            assertThat(savedPlan.getLastDoneAt()).isEqualTo(today);
            assertThat(savedPlan.getLastDoneKm()).isEqualTo(52500);
            assertThat(savedPlan.getNextDueAt()).isEqualTo(today.plusDays(180));
            assertThat(savedPlan.getNextDueKm()).isEqualTo(62500);
            assertThat(savedPlan.getNextDueAt()).isAfter(LocalDate.of(2026, 1, 1));
            assertThat(savedPlan.getNextDueKm()).isGreaterThan(52500);
        }

        @Test
        void completeOrder_should_throwInvalidOrderState_when_statusIsPending() {
            MaintenanceOrder order = baseOrder();
            order.setStatus(MaintenanceOrderStatus.PENDING);
            CompleteOrderRequest request = new CompleteOrderRequest(
                    new BigDecimal("100.00"), 51000, "Taller", "Notas"
            );
            when(maintenanceOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.completeOrder(orderId, request))
                    .isInstanceOf(InvalidOrderState.class)
                    .hasMessageContaining("curso");
            verify(vehicleClient, never()).getVehicle(any(UUID.class));
            verify(maintenanceOrderRepository, never()).save(any(MaintenanceOrder.class));
        }

        @Test
        void completeOrder_should_throwInvalidOdometer_when_finalOdometerIsLowerThanVehicleOdometer() {
            MaintenanceOrder order = baseOrder();
            order.setStatus(MaintenanceOrderStatus.IN_PROGRESS);
            CompleteOrderRequest request = new CompleteOrderRequest(
                    new BigDecimal("100.00"), 40000, "Taller", "Notas"
            );
            when(maintenanceOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicleSnapshot(VehicleStatus.IN_MAINTENANCE, 50000));

            assertThatThrownBy(() -> service.completeOrder(orderId, request))
                    .isInstanceOf(InvalidOdometerException.class)
                    .hasMessageContaining("odómetro");
            verify(maintenanceOrderRepository, never()).save(any(MaintenanceOrder.class));
            verify(maintenancePlanRespository, never()).save(any(MaintenancePlan.class));
        }

        @Test
        void completeOrder_should_useVehicleOdometerAsFinal_when_requestOdometerIsNull() {
            MaintenanceOrder order = baseOrder();
            order.setStatus(MaintenanceOrderStatus.IN_PROGRESS);
            MaintenancePlan plan = basePlan();

            CompleteOrderRequest request = new CompleteOrderRequest(
                    new BigDecimal("100.00"), null, "Taller", "Notas"
            );
            when(maintenanceOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicleSnapshot(VehicleStatus.IN_MAINTENANCE, 51000));
            when(maintenancePlanRespository.findById(planId)).thenReturn(Optional.of(plan));
            when(maintenancePlanRespository.save(any(MaintenancePlan.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(maintenanceOrderRepository.save(any(MaintenanceOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.completeOrder(orderId, request);

            assertThat(order.getOdometerKm()).isEqualTo(51000);
            verify(vehicleClient).updateVehicle(any(UUID.class), any(StatusChangeRequest.class));
        }
    }

    private MaintenancePlan basePlan() {
        MaintenancePlan plan = new MaintenancePlan();
        plan.setId(planId);
        plan.setVehicleId(vehicleId);
        plan.setType(MaintenanceType.OIL_CHANGE);
        plan.setIntervalKm(10000);
        plan.setIntervalDays(180);
        plan.setActive(true);
        plan.setCreatedAt(Instant.now());
        return plan;
    }

    private MaintenanceOrder baseOrder() {
        return MaintenanceOrder.builder()
                .id(orderId)
                .planId(planId)
                .vehicleId(vehicleId)
                .type(MaintenanceType.OIL_CHANGE)
                .status(MaintenanceOrderStatus.PENDING)
                .scheduledFor(LocalDate.of(2026, 1, 1))
                .createdAt(Instant.now())
                .build();
    }

    private StatusSnapshot vehicleSnapshot(int odometerKm) {
        return vehicleSnapshot(VehicleStatus.AVAILABLE, odometerKm);
    }

    private StatusSnapshot vehicleSnapshot(VehicleStatus status, int odometerKm) {
        return new StatusSnapshot(vehicleId, odometerKm, status);
    }

    private PlanDetailsResponse anyPlanDetailsResponse() {
        return new PlanDetailsResponse(
                planId, vehicleId, MaintenanceType.BRAKE_CHECK,
                5000, 90, LocalDate.now(ZoneOffset.UTC), 75000,
                LocalDate.now(ZoneOffset.UTC).plusDays(90), 80000, true
        );
    }
}
