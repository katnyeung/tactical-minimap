package org.tactical.minimap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
@EnableTransactionManagement
public class MinimapApplication {

	public static void main(String[] args) {
		SpringApplication.run(MinimapApplication.class, args);
	}

}
