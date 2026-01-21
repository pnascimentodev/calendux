package pnascimento.dev.v1;

import org.springframework.boot.SpringApplication;

public class TestAgenduxApplication {

	public static void main(String[] args) {
		SpringApplication.from(AgenduxApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
