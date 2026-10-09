package es.bytescolab.msroutes.config;

import feign.Client;
import feign.RequestInterceptor;
import feign.okhttp.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalKeyInterceptor(@Value("${internal.api-key}") String apiKey) {
        return template -> template.header("X-Internal-Key", apiKey);
    }

    @Bean
    public Client feignClient() {
        return new OkHttpClient();
    }
}