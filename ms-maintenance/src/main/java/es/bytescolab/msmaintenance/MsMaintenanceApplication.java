package es.bytescolab.msmaintenance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "es.bytescolab.msmaintenance.client")
public class MsMaintenanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsMaintenanceApplication.class, args);
    }

}
