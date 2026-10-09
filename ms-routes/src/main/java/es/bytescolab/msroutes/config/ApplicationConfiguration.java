package es.bytescolab.msroutes.config;

import es.bytescolab.msroutes.client.DriverClient;
import es.bytescolab.msroutes.client.VehicleClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@EnableFeignClients(basePackageClasses = {VehicleClient.class, DriverClient.class})
public class ApplicationConfiguration {
}
