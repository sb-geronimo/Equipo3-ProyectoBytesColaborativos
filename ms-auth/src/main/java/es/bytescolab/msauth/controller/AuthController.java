package es.bytescolab.msauth.controller;

import es.bytescolab.msauth.dto.request.LoginRequest;
import es.bytescolab.msauth.dto.request.RegisterRequest;
import es.bytescolab.msauth.dto.response.AuthResponse;
import es.bytescolab.msauth.dto.response.ErrorResponse;
import es.bytescolab.msauth.dto.response.RegisterResponse;
import es.bytescolab.msauth.dto.response.ValidateResponse;
import es.bytescolab.msauth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y validación de tokens JWT")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Registrar un gestor de flota",
            description = "Crea un usuario con rol MANAGER. La contraseña se guarda con bcrypt. "
                    + "Debe tener al menos 8 caracteres, una mayúscula y un número.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Contraseña inválida", value = """
                                    {
                                      "error": "VALIDATION_ERROR",
                                      "message": "La petición contiene campos no válidos",
                                      "details": [
                                        {"field": "password", "reason": "La contraseña debe contener al menos una mayúscula y un número"}
                                      ],
                                      "timestamp": "2026-10-02T10:15:30Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "409", description = "El usuario ya existe (USER_ALREADY_EXISTS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Email duplicado", value = """
                                    {
                                      "error": "USER_ALREADY_EXISTS",
                                      "message": "Ya existe un usuario con este email",
                                      "timestamp": "2026-10-02T10:15:30Z"
                                    }
                                    """)))
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid
            @RequestBody RegisterRequest request) {
        log.info("Register request: {}", request.email());
        RegisterResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Iniciar sesión",
            description = "Devuelve un JWT con validez de 3600 segundos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Email inválido", value = """
                                    {
                                      "error": "VALIDATION_ERROR",
                                      "message": "La petición contiene campos no válidos",
                                      "details": [
                                        {"field": "email", "reason": "Formato de email invalido"}
                                      ],
                                      "timestamp": "2026-10-02T10:15:30Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas (INVALID_CREDENTIALS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Credenciales incorrectas", value = """
                                    {
                                      "error": "INVALID_CREDENTIALS",
                                      "message": "Email o contraseña incorrectos",
                                      "timestamp": "2026-10-02T10:15:30Z"
                                    }
                                    """)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid
            @RequestBody LoginRequest request) {
        log.info("Login request: {}", request.email());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Validar un token JWT",
            description = "Devuelve los datos del usuario cuando el token es válido. "
                    + "Pensado para llamadas entre servicios (admite la cabecera X-Internal-Key).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token válido",
                    content = @Content(schema = @Schema(implementation = ValidateResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o caducado",
                    content = @Content(schema = @Schema(implementation = ValidateResponse.class),
                            examples = {
                                    @ExampleObject(name = "Token caducado", value = """
                                            {
                                              "valid": false,
                                              "error": "TOKEN_EXPIRED",
                                              "message": "El token ha expirado"
                                            }
                                            """),
                                    @ExampleObject(name = "Sin token", value = """
                                            {
                                              "valid": false,
                                              "error": "UNAUTHORIZED",
                                              "message": "Token no encontrado o formato inválido"
                                            }
                                            """)
                            }))
    })
    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(
            @Parameter(description = "JWT con prefijo Bearer", example = "Bearer eyJhbGciOiJIUzI1NiJ9...")
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        ValidateResponse response = authService.validate(token);
        HttpStatus status = response.valid() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(response);
    }
}
