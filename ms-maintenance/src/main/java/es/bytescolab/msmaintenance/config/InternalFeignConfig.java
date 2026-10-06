package es.bytescolab.msmaintenance.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalKeyInterceptor(
            @Value("${internal.api-key}") String apiKey
    ) {
        return template -> template.header("X-Internal-Key", apiKey);
    }
}
