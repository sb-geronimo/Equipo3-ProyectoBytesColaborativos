package es.bytescolab.msauth.service.impl;

import es.bytescolab.msauth.dto.request.LoginRequest;
import es.bytescolab.msauth.dto.request.RegisterRequest;
import es.bytescolab.msauth.dto.response.AuthResponse;
import es.bytescolab.msauth.dto.response.RegisterResponse;
import es.bytescolab.msauth.dto.response.ValidateResponse;
import es.bytescolab.msauth.entity.User;
import es.bytescolab.msauth.exception.InvalidCredentialsException;
import es.bytescolab.msauth.exception.UserAlreadyExists;
import es.bytescolab.msauth.mapper.UserMapper;
import es.bytescolab.msauth.repository.UserRepository;
import es.bytescolab.msauth.security.JwtUtil;
import es.bytescolab.msauth.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        User user = userMapper.toEntity(request);

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UserAlreadyExists("Ya existe un usuario con este username");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExists("Ya existe un usuario con este email");
        }

        User userWithEncodePassword = User.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .password(passwordEncoder.encode(request.password()))
                .role(user.getRole())
                .build();

        User saved = userRepository.save(userWithEncodePassword);

        return userMapper.toRegisterResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("Email o contraseña incorrectos");
        }

        // Aqui se reutiliza el usuario autenticado (NO se hace otra query a db)
        User user = (User) authentication.getPrincipal();

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiration / 1000)
                .userId(user.getId().toString())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public ValidateResponse validate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ValidateResponse.builder()
                    .valid(false)
                    .error("UNAUTHORIZED")
                    .message("Token no encontrado o formato inválido")
                    .build();
        }

        String token = authorizationHeader.substring(7);

        try {
            Claims claims = jwtUtil.parse(token);
            return ValidateResponse.builder()
                    .valid(true)
                    .userId(claims.getSubject())
                    .username(claims.get("username", String.class))
                    .role(claims.get("role", String.class))
                    .build();
        } catch (ExpiredJwtException e) {
            return ValidateResponse.builder()
                    .valid(false)
                    .error("TOKEN_EXPIRED")
                    .message("El token ha expirado")
                    .build();
        } catch (JwtException e) {
            return ValidateResponse.builder()
                    .valid(false)
                    .error("UNAUTHORIZED")
                    .message("Token inválido o firma incorrecta")
                    .build();
        }
    }
}
