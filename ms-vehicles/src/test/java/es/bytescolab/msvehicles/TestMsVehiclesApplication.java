package es.bytescolab.msvehicles;

import org.springframework.boot.SpringApplication;

public class TestMsVehiclesApplication {

    public static void main(String[] args) {
        SpringApplication.from(MsVehiclesApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
