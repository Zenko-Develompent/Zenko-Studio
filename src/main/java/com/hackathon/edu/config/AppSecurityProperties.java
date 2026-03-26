package com.hackathon.edu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {
    private String masterKeyB64 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private long accessTtlSec = 900;
    private long refreshTtlDays = 30;
    private long sessionTtlSec = 86400;
    private long loginRateLimitMax = 8;
    private long loginRateLimitWindowMs = 300_000;
    private Cookies cookies = new Cookies();
    private Cors cors = new Cors();

    @Data
    public static class Cookies {
        private boolean secure = false;
        private String domain = "";
    }

    @Data
    public static class Cors {
        private boolean allowAll = true;
    }
}
