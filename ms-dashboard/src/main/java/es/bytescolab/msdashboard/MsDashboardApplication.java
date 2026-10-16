package es.bytescolab.msdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsDashboardApplication.class, args);
    }

}
