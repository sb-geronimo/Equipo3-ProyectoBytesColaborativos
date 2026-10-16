package es.bytescolab.msmaintenance;

import es.bytescolab.msmaintenance.client.VehicleClient;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusChangeRequest;
import es.bytescolab.msmaintenance.client.vehicle.dto.StatusSnapshot;
import es.bytescolab.msmaintenance.client.vehicle.enums.VehicleStatus;
import es.bytescolab.msmaintenance.entity.MaintenanceOrder;
import es.bytescolab.msmaintenance.entity.MaintenancePlan;
import es.bytescolab.msmaintenance.enums.MaintenanceOrderStatus;
import es.bytescolab.msmaintenance.enums.MaintenanceType;
import es.bytescolab.msmaintenance.repository.MaintenanceOrderRepository;
import es.bytescolab.msmaintenance.repository.MaintenancePlanRespository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "internal.api-key=test-internal-key",
                "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtaG1hYy1zaWduaW5n",
                "vehicles.service.url=http://localhost:18084"
        }
)
@Import(TestcontainersConfiguration.class)
class MaintenanceServiceIT {

    private static final String JWT_SECRET_BASE64 = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtaG1hYy1zaWduaW5n";
    private static final UUID USER_ID = UUID.fromString("9a4d2e10-7c4a-4b55-b1a0-3c8e6f7a9210");

    @LocalServerPort
    private int port;

    @Autowired
    private MaintenancePlanRespository planRepository;

    @Autowired
    private MaintenanceOrderRepository orderRepository;

    @MockitoBean
    private VehicleClient vehicleClient;

    private RestTemplate restTemplate;
    private UUID vehicleId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        planRepository.deleteAll();

        DefaultResponseErrorHandler nonThrowingHandler = new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        };
        ClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(nonThrowingHandler);

        vehicleId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        when(vehicleClient.getVehicle(vehicleId))
                .thenReturn(new StatusSnapshot(vehicleId, 50000, VehicleStatus.AVAILABLE));
    }

    @Test
    void fullLifecycle_should_createPlanStartAndCompleteOrder_advancingPlanNextDueValues() {
        Map<String, Object> planBody = new HashMap<>();
        planBody.put("vehicleId", vehicleId.toString());
        planBody.put("type", "OIL_CHANGE");
        planBody.put("intervalKm", 10000);
        planBody.put("intervalDays", 180);
        planBody.put("lastDoneAt", "2026-01-01");
        planBody.put("lastDoneKm", 50000);

        ResponseEntity<JsonNode> planResponse = exchange(
                HttpMethod.POST, "/api/maintenance/plans", planBody, JsonNode.class
        );

        assertThat(planResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode planBody1 = Objects.requireNonNull(planResponse.getBody());
        UUID planId = UUID.fromString(planBody1.get("id").asText());
        LocalDate originalNextDueAt = LocalDate.parse(planBody1.get("nextDueAt").asText());
        Integer originalNextDueKm = planBody1.get("nextDueKm").asInt();

        when(vehicleClient.getVehicle(vehicleId))
                .thenReturn(new StatusSnapshot(vehicleId, 55000, VehicleStatus.AVAILABLE));

        MaintenanceOrder order = orderRepository.save(MaintenanceOrder.builder()
                .planId(planId)
                .vehicleId(vehicleId)
                .type(MaintenanceType.OIL_CHANGE)
                .status(MaintenanceOrderStatus.PENDING)
                .scheduledFor(LocalDate.now(ZoneOffset.UTC))
                .build());
        UUID orderId = order.getId();

        ResponseEntity<JsonNode> startResponse = exchange(
                HttpMethod.POST, "/api/maintenance/orders/" + orderId + "/start", null, JsonNode.class
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode startedBody = Objects.requireNonNull(startResponse.getBody());
        assertThat(startedBody.get("status").asText()).isEqualTo("IN_PROGRESS");
        verify(vehicleClient).updateVehicle(eq(vehicleId), any(StatusChangeRequest.class));

        when(vehicleClient.getVehicle(vehicleId))
                .thenReturn(new StatusSnapshot(vehicleId, 60000, VehicleStatus.IN_MAINTENANCE));

        Map<String, Object> completeBody = new HashMap<>();
        completeBody.put("cost", new BigDecimal("250.00"));
        completeBody.put("odometerKm", 60000);
        completeBody.put("workshop", "Taller oficial");
        completeBody.put("notes", "Cambio de aceite y filtros");

        ResponseEntity<JsonNode> completeResponse = exchange(
                HttpMethod.POST, "/api/maintenance/orders/" + orderId + "/complete", completeBody, JsonNode.class
        );

        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode completedBody = Objects.requireNonNull(completeResponse.getBody());
        assertThat(completedBody.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(completedBody.get("odometerKm").asInt()).isEqualTo(60000);
        assertThat(completedBody.get("plan").get("id").asText()).isEqualTo(planId.toString());

        MaintenancePlan reloaded = planRepository.findById(planId).orElseThrow();
        LocalDate recalculatedNextDueAt = reloaded.getNextDueAt();
        Integer recalculatedNextDueKm = reloaded.getNextDueKm();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertThat(recalculatedNextDueAt).isNotNull();
        assertThat(recalculatedNextDueAt).isEqualTo(today.plusDays(180));
        assertThat(recalculatedNextDueAt).isAfter(originalNextDueAt);
        assertThat(recalculatedNextDueKm).isEqualTo(60000 + 10000);
        assertThat(recalculatedNextDueKm).isGreaterThan(60000);
        assertThat(recalculatedNextDueKm).isGreaterThan(originalNextDueKm);
        assertThat(reloaded.getLastDoneAt()).isEqualTo(today);
        assertThat(reloaded.getLastDoneKm()).isEqualTo(60000);
    }

    @Test
    void createPlan_should_return409PlanAlreadyExists_when_secondActivePlanForSameVehicleAndType() {
        Map<String, Object> firstPlanBody = new HashMap<>();
        firstPlanBody.put("vehicleId", vehicleId.toString());
        firstPlanBody.put("type", "OIL_CHANGE");
        firstPlanBody.put("intervalKm", 10000);
        firstPlanBody.put("intervalDays", 180);
        firstPlanBody.put("lastDoneAt", "2026-01-01");
        firstPlanBody.put("lastDoneKm", 50000);

        ResponseEntity<JsonNode> firstResponse = exchange(
                HttpMethod.POST, "/api/maintenance/plans", firstPlanBody, JsonNode.class
        );
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> secondPlanBody = new HashMap<>();
        secondPlanBody.put("vehicleId", vehicleId.toString());
        secondPlanBody.put("type", "OIL_CHANGE");
        secondPlanBody.put("intervalKm", 8000);
        secondPlanBody.put("intervalDays", 150);
        secondPlanBody.put("lastDoneAt", "2026-02-01");
        secondPlanBody.put("lastDoneKm", 52000);

        ResponseEntity<JsonNode> secondResponse = exchange(
                HttpMethod.POST, "/api/maintenance/plans", secondPlanBody, JsonNode.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode errorBody = Objects.requireNonNull(secondResponse.getBody());
        assertThat(errorBody.get("error").asText()).isEqualTo("PLAN_ALREADY_EXISTS");
        assertThat(errorBody.get("message").asText()).contains("plan activo");

        long planCount = planRepository.findAll().stream()
                .filter(p -> p.getVehicleId().equals(vehicleId))
                .filter(p -> p.getType() == MaintenanceType.OIL_CHANGE)
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .count();
        assertThat(planCount).isEqualTo(1);
    }

    private <T> ResponseEntity<T> exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateTestJwt());

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                method,
                entity,
                responseType
        );
    }

    private String generateTestJwt() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET_BASE64));
        long nowMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(USER_ID.toString())
                .claim("role", "MANAGER")
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + 3_600_000))
                .signWith(key)
                .compact();
    }
}
