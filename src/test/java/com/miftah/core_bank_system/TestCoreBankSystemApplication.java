package com.miftah.core_bank_system;

import org.springframework.boot.SpringApplication;

public class TestCoreBankSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoreBankSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
