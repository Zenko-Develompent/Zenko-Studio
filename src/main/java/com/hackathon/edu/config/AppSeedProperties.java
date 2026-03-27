package com.hackathon.edu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.seed")
public class AppSeedProperties {
    private boolean enabled = false;
    /**
     * When enabled, seed runs only if it looks like the DB hasn't been seeded yet.
     * This is a safety switch to avoid overwriting data (including admin password) on every restart.
     */
    private boolean onlyIfEmpty = true;

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

