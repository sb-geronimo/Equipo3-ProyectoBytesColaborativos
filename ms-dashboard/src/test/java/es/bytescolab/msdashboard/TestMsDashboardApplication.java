package es.bytescolab.msdashboard;

import org.springframework.boot.SpringApplication;

public class TestMsDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.from(MsDashboardApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
