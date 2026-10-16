package es.bytescolab.msdrivers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "internal.api-key=test-internal-key",
                "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtaG1hYy1zaWduaW5n"
        }
)
@Import(TestcontainersConfiguration.class)
class DriverServiceIT {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";
    private static final String INTERNAL_KEY_VALUE = "test-internal-key";
    private static final String UUID_SUFFIX = UUID.randomUUID().toString().substring(0, 8);

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        DefaultResponseErrorHandler nonThrowingHandler = new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        };
        ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(nonThrowingHandler);
    }

    @Test
    @DisplayName("create should return 201 with persisted driver and fetch by id should return 200 with same shape")
    void createAndFindById_should_returnPersistedDriver_overHttp() {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Sara Ibáñez");
        body.put("email", "sara.ibanez@" + UUID_SUFFIX + ".com");
        body.put("phone", "+34 622 456 789");
        body.put("licenseNumber", "B-" + UUID_SUFFIX);
        body.put("licenseCategory", "B");
        body.put("licenseExpiresAt", LocalDate.now().plusYears(3).toString());

        ResponseEntity<JsonNode> createResponse = exchange(
                HttpMethod.POST, "/api/drivers", body, JsonNode.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = Objects.requireNonNull(createResponse.getBody());
        String createdId = created.get("id").asText();
        assertThat(createdId).isNotBlank();
        assertThat(created.get("licenseNumber").asText()).isEqualTo(body.get("licenseNumber"));
        assertThat(created.get("status").asText()).isEqualTo("ACTIVE");

        ResponseEntity<JsonNode> findResponse = exchange(
                HttpMethod.GET, "/api/drivers/" + createdId, null, JsonNode.class
        );

        assertThat(findResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode fetched = Objects.requireNonNull(findResponse.getBody());
        assertThat(fetched.get("id").asText()).isEqualTo(createdId);
        assertThat(fetched.get("fullName").asText()).isEqualTo("Sara Ibáñez");
        assertThat(fetched.get("licenseNumber").asText()).isEqualTo(body.get("licenseNumber"));
        assertThat(fetched.get("createdAt").asText()).isNotBlank();
        assertThat(fetched.get("updatedAt").asText()).isNotBlank();
    }

    @Test
    @DisplayName("create should return 409 when creating two drivers with the same license number")
    void create_should_returnConflict_when_licenseNumberDuplicated() {
        String sharedLicense = "B-DUP-" + UUID_SUFFIX;
        Map<String, Object> first = new HashMap<>();
        first.put("fullName", "Sara Ibáñez");
        first.put("email", "sara.it@" + UUID_SUFFIX + ".com");
        first.put("phone", "+34 622 456 789");
        first.put("licenseNumber", sharedLicense);
        first.put("licenseCategory", "B");
        first.put("licenseExpiresAt", LocalDate.now().plusYears(3).toString());

        ResponseEntity<JsonNode> firstResponse = exchange(
                HttpMethod.POST, "/api/drivers", first, JsonNode.class
        );
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> second = new HashMap<>();
        second.put("fullName", "Laura Fernández");
        second.put("email", "laura.it@" + UUID_SUFFIX + ".com");
        second.put("phone", "+34 600 123 456");
        second.put("licenseNumber", sharedLicense);
        second.put("licenseCategory", "B");
        second.put("licenseExpiresAt", LocalDate.now().plusYears(3).toString());

        ResponseEntity<JsonNode> secondResponse = exchange(
                HttpMethod.POST, "/api/drivers", second, JsonNode.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode error = Objects.requireNonNull(secondResponse.getBody());
        assertThat(error.get("error").asText()).isEqualTo("DRIVER_ALREADY_EXISTS");
        assertThat(error.get("message").asText()).contains("licencia");
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
