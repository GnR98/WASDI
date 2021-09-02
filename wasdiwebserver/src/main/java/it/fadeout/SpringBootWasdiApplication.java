package it.fadeout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootWasdiApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringBootWasdiApplication.class);

	public static void main(String[] args) {
		LOGGER.info("------- SpringBootWasdiApplication starting -------");

		SpringApplication.run(SpringBootWasdiApplication.class, args);
	}

}
