package es.bytescolab.msdrivers;

import org.springframework.boot.SpringApplication;

public class TestMsDriversApplication {

    public static void main(String[] args) {
        SpringApplication.from(MsDriversApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
