package com.hackathon.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import lombok.Value;

@SpringBootApplication
public class EduPlatformApplication {

	@Value("${DB_URL}")
    private String dbUrl;

	public static void main(String[] args) {
		SpringApplication.run(EduPlatformApplication.class, args);
	}

	@PostConstruct
    public void init() {
        System.out.println("DB URL: " + dbUrl);
    }

}
