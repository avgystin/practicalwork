package BellSpring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Включение поддержки планировщика
public class BellSpring {

	public static void main(String[] args) {
		SpringApplication.run(BellSpring.class, args);
	}

}
