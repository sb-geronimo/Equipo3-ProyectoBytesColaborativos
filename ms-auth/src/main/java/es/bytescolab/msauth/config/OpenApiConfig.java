package es.bytescolab.msauth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "FleetControl · ms-auth",
        version = "v1",
        description = "Registro, inicio de sesión y validación de tokens JWT."))
public class OpenApiConfig {
}
