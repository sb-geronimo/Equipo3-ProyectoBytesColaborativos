package es.bytescolab.msvehicles;

import es.bytescolab.msvehicles.enums.FuelType;
import es.bytescolab.msvehicles.enums.VehicleType;
import es.bytescolab.msvehicles.repository.VehicleRepository;
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
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "internal.api-key=test-internal-key",
                "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtaG1hYy1zaWduaW5n"
        }
)
@Import(TestcontainersConfiguration.class)
class VehicleServiceIT {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";
    private static final String INTERNAL_KEY_VALUE = "test-internal-key";

    @LocalServerPort
    private int port;

    @Autowired
    private VehicleRepository vehicleRepository;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();

        DefaultResponseErrorHandler nonThrowingHandler = new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        };
        ClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(nonThrowingHandler);
    }

    @Test
    @DisplayName("create then find then patch status should return 201 then 200 then 200 with matching data")
    void createAndFindAndPatchStatus_should_returnExpectedCodesAndPayload() {
        String plate = uniquePlate();
        Map<String, Object> createBody = buildVehicleBody(plate);

        ResponseEntity<JsonNode> createResponse = exchange(
                HttpMethod.POST, "/api/vehicles", createBody, JsonNode.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = Objects.requireNonNull(createResponse.getBody());
        String createdId = created.get("id").asText();
        assertThat(createdId).isNotBlank();
        assertThat(created.get("plate").asText()).isEqualTo(plate);
        assertThat(created.get("status").asText()).isEqualTo("AVAILABLE");
        assertThat(created.get("odometerKm").asInt()).isEqualTo(15000);

        ResponseEntity<JsonNode> findResponse = exchange(
                HttpMethod.GET, "/api/vehicles/" + createdId, null, JsonNode.class
        );

        assertThat(findResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode fetched = Objects.requireNonNull(findResponse.getBody());
        assertThat(fetched.get("id").asText()).isEqualTo(createdId);
        assertThat(fetched.get("plate").asText()).isEqualTo(plate);
        assertThat(fetched.get("status").asText()).isEqualTo("AVAILABLE");

        Map<String, Object> statusBody = new HashMap<>();
        statusBody.put("status", "IN_USE");
        statusBody.put("odometerKm", 16000);

        ResponseEntity<JsonNode> patchResponse = exchange(
                HttpMethod.PATCH, "/api/vehicles/" + createdId + "/status", statusBody, JsonNode.class
        );

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode patched = Objects.requireNonNull(patchResponse.getBody());
        assertThat(patched.get("id").asText()).isEqualTo(createdId);
        assertThat(patched.get("status").asText()).isEqualTo("IN_USE");
        assertThat(patched.get("odometerKm").asInt()).isEqualTo(16000);
    }

    @Test
    @DisplayName("create should return 409 VEHICLE_ALREADY_EXISTS when creating two vehicles with the same plate")
    void create_should_return409_when_plateDuplicated() {
        String sharedPlate = uniquePlate();
        Map<String, Object> first = buildVehicleBody(sharedPlate);

        ResponseEntity<JsonNode> firstResponse = exchange(
                HttpMethod.POST, "/api/vehicles", first, JsonNode.class
        );
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> second = buildVehicleBody(sharedPlate);
        second.put("make", "Honda");

        ResponseEntity<JsonNode> secondResponse = exchange(
                HttpMethod.POST, "/api/vehicles", second, JsonNode.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode error = Objects.requireNonNull(secondResponse.getBody());
        assertThat(error.get("error").asText()).isEqualTo("VEHICLE_ALREADY_EXISTS");
        assertThat(error.get("message").asText()).contains(sharedPlate);
    }

    @Test
    @DisplayName("patch status should return 409 INVALID_ODOMETER when new odometer is lower than current")
    void patchStatus_should_return409InvalidOdometer_when_odometerRegresses() {
        String plate = uniquePlate();
        Map<String, Object> createBody = buildVehicleBody(plate);

        ResponseEntity<JsonNode> createResponse = exchange(
                HttpMethod.POST, "/api/vehicles", createBody, JsonNode.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String createdId = Objects.requireNonNull(createResponse.getBody()).get("id").asText();

        Map<String, Object> regressBody = new HashMap<>();
        regressBody.put("status", "IN_USE");
        regressBody.put("odometerKm", 1000);

        ResponseEntity<JsonNode> patchResponse = exchange(
                HttpMethod.PATCH, "/api/vehicles/" + createdId + "/status", regressBody, JsonNode.class
        );

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode error = Objects.requireNonNull(patchResponse.getBody());
        assertThat(error.get("error").asText()).isEqualTo("INVALID_ODOMETER");
        assertThat(error.get("message").asText()).contains("odómetro");
    }

    private Map<String, Object> buildVehicleBody(String plate) {
        Map<String, Object> body = new HashMap<>();
        body.put("plate", plate);
        body.put("make", "Toyota");
        body.put("model", "Corolla");
        body.put("year", 2020);
        body.put("type", VehicleType.CAR.name());
        body.put("fuelType", FuelType.GASOLINE.name());
        body.put("tankCapacityL", 50);
        body.put("odometerKm", 15000);
        return body;
    }

    private String uniquePlate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int digits = random.nextInt(1000, 10000);
        char first = (char) ('A' + random.nextInt(26));
        char middle = (char) ('A' + random.nextInt(26));
        char last = (char) ('A' + random.nextInt(26));
        return digits + "-" + first + middle + last;
    }

    private <T> ResponseEntity<T> exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_KEY_HEADER, INTERNAL_KEY_VALUE);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                method,
                entity,
                responseType
        );
    }
}
