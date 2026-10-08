package es.bytescolab.msroutes.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {

    @Value("${internal.api-key:}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalKeyInterceptor() {
        return template -> template.header("X-Internal-Key", internalApiKey);
    }

    @Bean
    public FeignErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }
}