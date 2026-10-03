package es.bytescolab.msroutes;

import org.springframework.boot.SpringApplication;

public class TestMsRoutesApplication {

    public static void main(String[] args) {
        SpringApplication.from(MsRoutesApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
