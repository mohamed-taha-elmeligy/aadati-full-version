package com.mts.aadati;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableScheduling
@SpringBootApplication
@EnableTransactionManagement
@EnableAsync
@ConfigurationPropertiesScan
public class AadatiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AadatiApplication.class, args);
	}
}