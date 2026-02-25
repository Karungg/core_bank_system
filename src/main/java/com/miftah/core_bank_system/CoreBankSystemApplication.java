package com.miftah.core_bank_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CoreBankSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreBankSystemApplication.class, args);
	}

}
