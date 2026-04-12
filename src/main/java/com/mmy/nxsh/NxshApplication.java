package com.mmy.nxsh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NxshApplication {

	public static void main(String[] args) {
		SpringApplication.run(NxshApplication.class, args);
	}

}
