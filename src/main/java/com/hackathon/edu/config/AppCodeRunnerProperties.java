package com.hackathon.edu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.code-runner")
public class AppCodeRunnerProperties {
    private boolean enabled = true;
    private String dockerCommand = "docker";
    private long timeoutMs = 5000;
    private int memoryMb = 128;
    private double cpus = 0.5d;
    private int pidsLimit = 64;
    private int maxCodeLength = 20000;
    private int maxInputLength = 4000;
    private int maxOutputLength = 20000;
}


//zenko