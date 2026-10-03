package es.bytescolab.msalerts;

import org.springframework.boot.SpringApplication;

public class TestMsAlertsApplication {

    public static void main(String[] args) {
        SpringApplication.from(MsAlertsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
