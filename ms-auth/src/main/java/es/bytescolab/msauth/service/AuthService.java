package es.bytescolab.msauth.service;

import es.bytescolab.msauth.dto.request.LoginRequest;
import es.bytescolab.msauth.dto.request.RegisterRequest;
import es.bytescolab.msauth.dto.response.AuthResponse;
import es.bytescolab.msauth.dto.response.RegisterResponse;
import es.bytescolab.msauth.dto.response.ValidateResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    ValidateResponse validate(String authorizationHeader);
}
