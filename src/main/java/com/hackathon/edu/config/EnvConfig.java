package com.hackathon.edu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class EnvConfig {

    @Value("${DB_URL}")
    private String dbUrl;

    @PostConstruct
    public void init() {
        System.out.println("DB URL: " + dbUrl);
    }
}