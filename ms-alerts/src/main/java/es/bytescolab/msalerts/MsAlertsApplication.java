package es.bytescolab.msalerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableRetry
@EnableJpaAuditing
@EnableFeignClients(basePackages = "es.bytescolab.msalerts.client")
@SpringBootApplication
public class MsAlertsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAlertsApplication.class, args);
    }
}