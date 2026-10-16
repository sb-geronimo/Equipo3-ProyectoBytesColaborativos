package es.bytescolab.msfuel;

import es.bytescolab.msfuel.client.VehicleClient;
import es.bytescolab.msfuel.dto.response.VehicleDetailResponse;
import es.bytescolab.msfuel.enums.FuelType;
import es.bytescolab.msfuel.enums.VehicleType;
import es.bytescolab.msfuel.repository.FuelRecordRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "internal.api-key=test-internal-key",
                "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtaG1hYy1zaWduaW5n",
                "clients.vehicle-service=http://localhost:0"
        }
)
@Import(TestcontainersConfiguration.class)
class FuelRecordServiceIT {

    private static final String JWT_SECRET_BASE64 = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtaG1hYy1zaWduaW5n";
    private static final UUID USER_ID = UUID.fromString("9a4d2e10-7c4a-4b55-b1a0-3c8e6f7a9210");

    @LocalServerPort
    private int port;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @MockitoBean
    private VehicleClient vehicleClient;

    private RestTemplate restTemplate;
    private UUID vehicleId;

    @BeforeEach
    void setUp() {
        fuelRecordRepository.deleteAll();

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

        VehicleDetailResponse vehicle = VehicleDetailResponse.builder()
                .id(vehicleId)
                .type(VehicleType.VAN)
                .fuelType(FuelType.DIESEL)
                .tankCapacityL(80)
                .odometerKm(45000)
                .build();
        when(vehicleClient.getVehicle(vehicleId)).thenReturn(vehicle);
    }

    @Test
    void create_should_return201_andComputedConsumption_when_twoFullTankRefuels() {
        Map<String, Object> firstFullTankBody = buildCreateBody(
                "2026-10-01T12:00:00Z",
                new BigDecimal("50.00"),
                45000,
                true,
                "Repsol A-3 km 112"
        );

        ResponseEntity<JsonNode> firstResponse = exchange(
                HttpMethod.POST, "/api/fuel/refuels", firstFullTankBody, JsonNode.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode firstCreated = Objects.requireNonNull(firstResponse.getBody());
        String firstId = firstCreated.get("id").asText();
        assertThat(firstId).isNotBlank();
        assertThat(firstCreated.get("vehicleId").asText()).isEqualTo(vehicleId.toString());
        assertThat(firstCreated.get("fullTank").asBoolean()).isTrue();
        assertThat(firstCreated.get("consumptionL100km").isNull()).isTrue();

        Map<String, Object> partialBody = buildCreateBody(
                "2026-10-04T12:00:00Z",
                new BigDecimal("20.00"),
                45250,
                false,
                "Repsol A-3 km 112"
        );

        ResponseEntity<JsonNode> partialResponse = exchange(
                HttpMethod.POST, "/api/fuel/refuels", partialBody, JsonNode.class
        );

        assertThat(partialResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> secondFullTankBody = buildCreateBody(
                "2026-10-08T12:00:00Z",
                new BigDecimal("50.00"),
                45500,
                true,
                "Repsol A-3 km 112"
        );

        ResponseEntity<JsonNode> secondResponse = exchange(
                HttpMethod.POST, "/api/fuel/refuels", secondFullTankBody, JsonNode.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode secondCreated = Objects.requireNonNull(secondResponse.getBody());
        assertThat(secondCreated.get("consumptionL100km").decimalValue())
                .isEqualByComparingTo(new BigDecimal("4.00"));

        ResponseEntity<JsonNode> listResponse = exchange(
                HttpMethod.GET, "/api/fuel/refuels?vehicle=" + vehicleId, null, JsonNode.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listed = Objects.requireNonNull(listResponse.getBody());
        assertThat(listed.get("totalElements").asInt()).isEqualTo(3);
        assertThat(listed.get("content").isArray()).isTrue();
        assertThat(listed.get("content").size()).isEqualTo(3);
    }

    @Test
    void create_should_return409InvalidOdometer_when_secondRefuelHasLowerOdometer() {
        Map<String, Object> firstBody = buildCreateBody(
                "2026-10-01T12:00:00Z",
                new BigDecimal("50.00"),
                45500,
                true,
                "Repsol A-3 km 112"
        );

        ResponseEntity<JsonNode> firstResponse = exchange(
                HttpMethod.POST, "/api/fuel/refuels", firstBody, JsonNode.class
        );
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> lowerOdometerBody = buildCreateBody(
                "2026-10-08T12:00:00Z",
                new BigDecimal("50.00"),
                45000,
                true,
                "Repsol A-3 km 112"
        );

        ResponseEntity<JsonNode> lowerOdometerResponse = exchange(
                HttpMethod.POST, "/api/fuel/refuels", lowerOdometerBody, JsonNode.class
        );

        assertThat(lowerOdometerResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode error = Objects.requireNonNull(lowerOdometerResponse.getBody());
        assertThat(error.get("error").asText()).isEqualTo("INVALID_ODOMETER");
        assertThat(error.get("message").asText()).contains("odómetro");
    }

    private Map<String, Object> buildCreateBody(
            String refueledAt, BigDecimal liters, int odometerKm, boolean fullTank, String station) {
        Map<String, Object> body = new HashMap<>();
        body.put("vehicleId", vehicleId.toString());
        body.put("refueledAt", refueledAt);
        body.put("liters", liters);
        body.put("pricePerLiter", new BigDecimal("1.62"));
        body.put("odometerKm", odometerKm);
        body.put("fullTank", fullTank);
        body.put("station", station);
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
                .subject(USER_ID.toString())
                .claim("role", "MANAGER")
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + 3_600_000))
                .signWith(key)
                .compact();
    }
}