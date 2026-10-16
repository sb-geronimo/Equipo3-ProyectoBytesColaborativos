package es.bytescolab.msgateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "FleetControl · ms-gateway",
        version = "v1",
        description = "Punto de entrada único, enrutamiento y estado del sistema."))
public class OpenApiConfig {
}
