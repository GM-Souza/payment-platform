package com.grupo5.payment_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// teste de .JAR
@EnableScheduling
@SpringBootApplication
public class PaymentPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentPlatformApplication.class, args);
	}

}
