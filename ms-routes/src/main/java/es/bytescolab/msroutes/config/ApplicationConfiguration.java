package es.bytescolab.msroutes.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableFeignClients(basePackages = "es.bytescolab.msroutes.client")
@EnableJpaAuditing
@Configuration
public class ApplicationConfiguration {
}