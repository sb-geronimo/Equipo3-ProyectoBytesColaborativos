package es.bytescolab.msauth.service.impl;

import es.bytescolab.msauth.dto.request.LoginRequest;
import es.bytescolab.msauth.dto.request.RegisterRequest;
import es.bytescolab.msauth.dto.response.AuthResponse;
import es.bytescolab.msauth.dto.response.RegisterResponse;
import es.bytescolab.msauth.dto.response.ValidateResponse;
import es.bytescolab.msauth.entity.User;
import es.bytescolab.msauth.enums.UserRole;
import es.bytescolab.msauth.exception.InvalidCredentialsException;
import es.bytescolab.msauth.exception.UserAlreadyExists;
import es.bytescolab.msauth.mapper.UserMapper;
import es.bytescolab.msauth.repository.UserRepository;
import es.bytescolab.msauth.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final Long EXPIRATION_MS = 3_600_000L;

    @BeforeEach
    void setUpExpiration() {
        ReflectionTestUtils.setField(authService, "expiration", EXPIRATION_MS);
    }

    @Test
    @DisplayName("register should return RegisterResponse with persisted user when request is valid")
    void register_should_returnRegisterResponse_when_requestIsValid() {
        RegisterRequest request = new RegisterRequest(
                "carlos_ruiz",
                "carlos@fleetcontrol.com",
                "FleetPass123"
        );
        User mapped = User.builder()
                .username(request.username())
                .email(request.email())
                .role(UserRole.MANAGER)
                .build();
        User encoded = User.builder()
                .username(request.username())
                .email(request.email())
                .password("ENCODED")
                .role(UserRole.MANAGER)
                .build();
        UUID generatedId = UUID.fromString("3f2a9c1e-7b4d-4e6a-9d2f-1a8b5c6d7e90");
        User saved = User.builder()
                .id(generatedId)
                .username(request.username())
                .email(request.email())
                .password("ENCODED")
                .role(UserRole.MANAGER)
                .createdAt(Instant.parse("2026-10-05T10:30:00Z"))
                .updatedAt(Instant.parse("2026-10-05T10:30:00Z"))
                .build();
        RegisterResponse expected = new RegisterResponse(
                generatedId,
                request.username(),
                request.email(),
                UserRole.MANAGER,
                Instant.parse("2026-10-05T10:30:00Z")
        );

        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toRegisterResponse(saved)).thenReturn(expected);

        RegisterResponse actual = authService.register(request);

        assertThat(actual).isEqualTo(expected);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(request.password());
        verify(userRepository, never()).deleteAll();
    }

    @Test
    @DisplayName("register should throw UserAlreadyExists when username is already taken")
    void register_should_throwUserAlreadyExists_when_usernameExists() {
        RegisterRequest request = new RegisterRequest(
                "carlos_ruiz",
                "carlos@fleetcontrol.com",
                "FleetPass123"
        );
        User mapped = User.builder()
                .username(request.username())
                .email(request.email())
                .role(UserRole.MANAGER)
                .build();

        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExists.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register should throw UserAlreadyExists when email is already taken")
    void register_should_throwUserAlreadyExists_when_emailExists() {
        RegisterRequest request = new RegisterRequest(
                "carlos_ruiz",
                "carlos@fleetcontrol.com",
                "FleetPass123"
        );
        User mapped = User.builder()
                .username(request.username())
                .email(request.email())
                .role(UserRole.MANAGER)
                .build();

        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExists.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login should return AuthResponse with bearer token when credentials are valid")
    void login_should_returnAuthResponse_when_credentialsAreValid() {
        LoginRequest request = new LoginRequest("carlos@fleetcontrol.com", "FleetPass123");
        UUID userId = UUID.fromString("3f2a9c1e-7b4d-4e6a-9d2f-1a8b5c6d7e90");
        User principal = User.builder()
                .id(userId)
                .username("carlos_ruiz")
                .email(request.email())
                .password("ENCODED")
                .role(UserRole.MANAGER)
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(userId, "carlos_ruiz", UserRole.MANAGER))
                .thenReturn("jwt.token.value");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt.token.value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.userId()).isEqualTo(userId.toString());
        assertThat(response.role()).isEqualTo("MANAGER");
        assertThat(response.expiresIn()).isEqualTo(EXPIRATION_MS / 1000);
    }

    @Test
    @DisplayName("login should throw InvalidCredentialsException when authentication manager rejects credentials")
    void login_should_throwInvalidCredentialsException_when_authenticationFails() {
        LoginRequest request = new LoginRequest("carlos@fleetcontrol.com", "WrongPass1");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("incorrectos");

        verify(jwtUtil, never()).generateToken(any(), anyString(), any());
    }

    @Test
    @DisplayName("validate should return invalid response when Authorization header is null")
    void validate_should_returnInvalid_when_headerIsNull() {
        ValidateResponse response = authService.validate(null);

        assertThat(response.valid()).isFalse();
        assertThat(response.error()).isEqualTo("UNAUTHORIZED");
        assertThat(response.message()).isNotBlank();
        assertThat(response.userId()).isNull();
    }

    @Test
    @DisplayName("validate should return invalid response when Authorization header does not start with Bearer")
    void validate_should_returnInvalid_when_headerMissingBearerPrefix() {
        ValidateResponse response = authService.validate("Basic dXNlcjpwYXNz");

        assertThat(response.valid()).isFalse();
        assertThat(response.error()).isEqualTo("UNAUTHORIZED");
        verify(jwtUtil, never()).parse(anyString());
    }

    @Test
    @DisplayName("validate should return valid response with claims when token is well-formed")
    void validate_should_returnValidClaims_when_tokenIsValid() {
        Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject("3f2a9c1e-7b4d-4e6a-9d2f-1a8b5c6d7e90")
                .add("username", "carlos_ruiz")
                .add("role", "MANAGER")
                .build();

        when(jwtUtil.parse("jwt.token.value")).thenReturn(claims);

        ValidateResponse response = authService.validate("Bearer jwt.token.value");

        assertThat(response.valid()).isTrue();
        assertThat(response.userId()).isEqualTo("3f2a9c1e-7b4d-4e6a-9d2f-1a8b5c6d7e90");
        assertThat(response.username()).isEqualTo("carlos_ruiz");
        assertThat(response.role()).isEqualTo("MANAGER");
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("validate should return TOKEN_EXPIRED when jwt library throws ExpiredJwtException")
    void validate_should_returnTokenExpired_when_jwtIsExpired() {
        when(jwtUtil.parse("expired.token"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        ValidateResponse response = authService.validate("Bearer expired.token");

        assertThat(response.valid()).isFalse();
        assertThat(response.error()).isEqualTo("TOKEN_EXPIRED");
        assertThat(response.message()).contains("expirado");
    }

    @Test
    @DisplayName("validate should return UNAUTHORIZED when jwt library throws a generic JwtException")
    void validate_should_returnUnauthorized_when_jwtIsMalformed() {
        when(jwtUtil.parse("malformed.token"))
                .thenThrow(new MalformedJwtException("malformed"));

        ValidateResponse response = authService.validate("Bearer malformed.token");

        assertThat(response.valid()).isFalse();
        assertThat(response.error()).isEqualTo("UNAUTHORIZED");
        assertThat(response.message()).contains("inv");
    }
}
