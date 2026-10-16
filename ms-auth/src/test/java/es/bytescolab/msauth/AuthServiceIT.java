package es.bytescolab.msauth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import es.bytescolab.msauth.dto.request.LoginRequest;
import es.bytescolab.msauth.dto.request.RegisterRequest;
import es.bytescolab.msauth.repository.UserRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "jwt.secret=Zm9vZm9vZm9vZm9vZm9vZm9vZm9vZm9vZm9vZm9vZm9vZm9vZm9v",
        "jwt.expiration=3600000",
        "internal.api-key=test-internal-key"
})
class AuthServiceIT {

    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String LOGIN_PATH = "/api/auth/login";

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate;

    {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        });
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("register fresh user returns 201 with body, then login returns 200 with bearer token")
    void register_then_login_should_returnBearerToken() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@fleetcontrol.com";
        String username = "ituser" + UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest registerRequest = new RegisterRequest(username, email, "FleetPass123");

        ResponseEntity<String> registerResponse = postJson(REGISTER_PATH, registerRequest);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode registerBody = objectMapper.readTree(registerResponse.getBody());
        assertThat(registerBody.get("id").asText()).isNotBlank();
        assertThat(registerBody.get("email").asText()).isEqualTo(email);
        assertThat(registerBody.get("username").asText()).isEqualTo(username);
        assertThat(registerBody.get("role").asText()).isEqualTo("MANAGER");
        assertThat(userRepository.existsByEmail(email)).isTrue();

        LoginRequest loginRequest = new LoginRequest(email, "FleetPass123");
        ResponseEntity<String> loginResponse = postJson(LOGIN_PATH, loginRequest);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode loginBody = objectMapper.readTree(loginResponse.getBody());
        assertThat(loginBody.get("token").asText()).isNotBlank();
        assertThat(loginBody.get("token").asText().split("\\.")).hasSize(3);
        assertThat(loginBody.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(loginBody.get("userId").asText()).isNotBlank();
        assertThat(loginBody.get("role").asText()).isEqualTo("MANAGER");
        assertThat(loginBody.get("expiresIn").asLong()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("registering the same email twice returns 409 USER_ALREADY_EXISTS via controller advice")
    void register_should_return409_when_emailAlreadyExists() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@fleetcontrol.com";
        String firstUsername = "firstuser" + UUID.randomUUID().toString().substring(0, 8);
        String secondUsername = "seconduser" + UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest first = new RegisterRequest(firstUsername, email, "FleetPass123");
        RegisterRequest duplicate = new RegisterRequest(secondUsername, email, "FleetPass456");

        ResponseEntity<String> firstResponse = postJson(REGISTER_PATH, first);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> duplicateResponse = postJson(REGISTER_PATH, duplicate);

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode errorBody = objectMapper.readTree(duplicateResponse.getBody());
        assertThat(errorBody.get("error").asText()).isEqualTo("USER_ALREADY_EXISTS");
        assertThat(errorBody.get("message").asText()).isNotBlank();
    }

    private ResponseEntity<String> postJson(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            String json = objectMapper.writeValueAsString(body);
            return restTemplate.exchange(
                    "http://localhost:" + port + path,
                    HttpMethod.POST,
                    new HttpEntity<>(json, headers),
                    String.class
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize request body for " + path, ex);
        }
    }
}
