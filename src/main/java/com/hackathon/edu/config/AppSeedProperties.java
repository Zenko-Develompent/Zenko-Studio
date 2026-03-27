package com.hackathon.edu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.seed")
public class AppSeedProperties {
    private boolean enabled = false;

    private Admin admin = new Admin();
    private Course course = new Course();

    @Data
    public static class Admin {
        private String username = "admin";
        private String password = "Admin1234";
        private int age = 30;
    }

    @Data
    public static class Course {
        private String name = "Demo Course";
        private String description = "Seeded course";
        private String category = "demo";
        private String moduleName = "Demo Module";
    }
}

