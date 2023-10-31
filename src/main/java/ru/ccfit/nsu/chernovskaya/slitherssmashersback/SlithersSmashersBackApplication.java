package ru.ccfit.nsu.chernovskaya.slitherssmashersback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SlithersSmashersBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(SlithersSmashersBackApplication.class, args);
	}
}
