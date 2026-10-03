package es.bytescolab.msfuel;

import org.springframework.boot.SpringApplication;

public class TestMsFuelApplication {

	public static void main(String[] args) {
		SpringApplication.from(MsFuelApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
