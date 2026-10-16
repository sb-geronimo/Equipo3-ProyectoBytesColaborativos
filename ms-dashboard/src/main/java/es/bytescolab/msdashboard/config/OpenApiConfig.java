package es.bytescolab.msdashboard.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "FleetControl · ms-dashboard",
        version = "v1",
        description = "Resumen ejecutivo y series temporales agregadas de la flota."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtenido en POST /api/auth/login. Cabecera: Authorization: Bearer <token>")
public class OpenApiConfig {
}
