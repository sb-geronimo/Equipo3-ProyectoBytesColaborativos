package es.bytescolab.msroutes;

import es.bytescolab.msroutes.client.DriverClient;
import es.bytescolab.msroutes.client.VehicleClient;
import es.bytescolab.msroutes.dto.response.DriverDetailResponse;
import es.bytescolab.msroutes.dto.response.VehicleDetailResponse;
import es.bytescolab.msroutes.enums.DriverStatus;
import es.bytescolab.msroutes.enums.LicenseCategory;
import es.bytescolab.msroutes.enums.VehicleStatus;
import es.bytescolab.msroutes.enums.VehicleType;
import es.bytescolab.msroutes.repository.RouteRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "internal.api-key=test-internal-key",
                "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtaG1hYy1zaWduaW5n",
                "clients.vehicle-service=http://localhost:0",
                "clients.driver-service=http://localhost:0"
        }
)
@Import(TestcontainersConfiguration.class)
class RouteServiceIT {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";
    private static final String INTERNAL_KEY_VALUE = "test-internal-key";
    private static final String JWT_SECRET_BASE64 = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtaG1hYy1zaWduaW5n";
    private static final UUID PLANNED_BY = UUID.fromString("9a4d2e10-7c4a-4b55-b1a0-3c8e6f7a9210");

    @LocalServerPort
    private int port;

    @Autowired
    private RouteRepository routeRepository;

    @MockitoBean
    private VehicleClient vehicleClient;

    @MockitoBean
    private DriverClient driverClient;

    private RestTemplate restTemplate;
    private UUID vehicleId;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        routeRepository.deleteAll();

        DefaultResponseErrorHandler nonThrowingHandler = new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        };
        ClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(nonThrowingHandler);

        vehicleId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        VehicleDetailResponse availableVehicle = VehicleDetailResponse.builder()
                .id(vehicleId)
                .type(VehicleType.VAN)
                .fuelType("DIESEL")
                .tankCapacityL(80)
                .odometerKm(45210)
                .status(VehicleStatus.AVAILABLE)
                .build();

        DriverDetailResponse activeDriver = DriverDetailResponse.builder()
                .id(driverId)
                .licenseCategory(LicenseCategory.B)
                .licenseExpiresAt(LocalDate.now().plusYears(3))
                .status(DriverStatus.ACTIVE)
                .build();

        when(vehicleClient.getVehicle(vehicleId)).thenReturn(availableVehicle);
        when(vehicleClient.updateStatus(eq(vehicleId), any())).thenReturn(availableVehicle);
        when(driverClient.getDriver(driverId)).thenReturn(activeDriver);
    }

    @Test
    @DisplayName("create then start then complete should transition PLANNED to IN_PROGRESS to COMPLETED with computed odometer")
    void createStartComplete_should_transitionFullLifecycle_overHttp() {
        Instant plannedStart = Instant.now().plusSeconds(86_400);
        Map<String, Object> createBody = buildCreateBody(plannedStart);

        ResponseEntity<JsonNode> createResponse = exchange(
                HttpMethod.POST, "/api/routes", createBody, JsonNode.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = Objects.requireNonNull(createResponse.getBody());
        String routeId = created.get("id").asText();
        assertThat(routeId).isNotBlank();
        assertThat(created.get("status").asText()).isEqualTo("PLANNED");

        ResponseEntity<JsonNode> startResponse = exchange(
                HttpMethod.POST, "/api/routes/" + routeId + "/start", null, JsonNode.class
        );

        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode started = Objects.requireNonNull(startResponse.getBody());
        assertThat(started.get("id").asText()).isEqualTo(routeId);
        assertThat(started.get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(started.get("startOdometerKm").asInt()).isEqualTo(45210);
        assertThat(started.get("startedAt").asText()).isNotBlank();

        Map<String, Object> completeBody = new HashMap<>();
        completeBody.put("actualDistanceKm", 362.40);
        completeBody.put("notes", "Sin incidencias");

        ResponseEntity<JsonNode> completeResponse = exchange(
                HttpMethod.POST, "/api/routes/" + routeId + "/complete", completeBody, JsonNode.class
        );

        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode completed = Objects.requireNonNull(completeResponse.getBody());
        assertThat(completed.get("id").asText()).isEqualTo(routeId);
        assertThat(completed.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(completed.get("endOdometerKm").asInt()).isEqualTo(45210 + 362);
        assertThat(completed.get("actualDistanceKm").asDouble()).isEqualTo(362.40);
        assertThat(completed.get("notes").asText()).isEqualTo("Sin incidencias");
        assertThat(completed.get("endedAt").asText()).isNotBlank();
    }

    @Test
    @DisplayName("create should return 409 ROUTE_OVERLAP when a second route overlaps the first for the same driver")
    void create_should_return409RouteOverlap_when_secondRouteOverlapsFirstForSameDriver() {
        Instant sharedStart = Instant.now().plusSeconds(86_400);
        Map<String, Object> firstBody = buildCreateBody(sharedStart);

        ResponseEntity<JsonNode> firstResponse = exchange(
                HttpMethod.POST, "/api/routes", firstBody, JsonNode.class
        );
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> overlappingBody = buildCreateBody(sharedStart.plusSeconds(1800));

        ResponseEntity<JsonNode> overlappingResponse = exchange(
                HttpMethod.POST, "/api/routes", overlappingBody, JsonNode.class
        );

        assertThat(overlappingResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode error = Objects.requireNonNull(overlappingResponse.getBody());
        assertThat(error.get("error").asText()).isEqualTo("ROUTE_OVERLAP");
        assertThat(error.get("message").asText()).contains("solapa");
    }

    private Map<String, Object> buildCreateBody(Instant plannedStart) {
        Map<String, Object> body = new HashMap<>();
        body.put("vehicleId", vehicleId.toString());
        body.put("driverId", driverId.toString());
        body.put("origin", "Madrid Centro Logístico");
        body.put("destination", "Valencia Puerto");
        body.put("plannedStart", plannedStart.toString());
        body.put("estimatedDurationMin", 240);
        body.put("plannedDistanceKm", new BigDecimal("355.0"));
        return body;
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
                .subject(PLANNED_BY.toString())
                .claim("role", "MANAGER")
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + 3_600_000))
                .signWith(key)
                .compact();
    }
}