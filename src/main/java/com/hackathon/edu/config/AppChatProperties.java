package com.hackathon.edu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Data
@Component
@ConfigurationProperties(prefix = "app.chat")
public class AppChatProperties {
    private String wsEndpoint = "/ws/social";
    private String wsAllowedOriginPatterns = "*";
    private String brokerDestinationPrefixes = "/topic,/queue";
    private String applicationDestinationPrefix = "/app";
    private String userDestinationPrefix = "/user";
    private String userChatDestination = "/queue/social/chat";
    private String userFriendDestination = "/queue/social/friends";
    private String userParentControlDestination = "/queue/social/parent-control";

    private int chatsDefaultLimit = 20;
    private int chatsMaxLimit = 100;
    private int messagesDefaultLimit = 50;
    private int messagesMaxLimit = 200;
    private int friendRequestsDefaultLimit = 20;
    private int friendRequestsMaxLimit = 50;
    private int friendsDefaultLimit = 50;
    private int friendsMaxLimit = 200;
    private int userSearchDefaultLimit = 20;
    private int userSearchMaxLimit = 50;
    private int userSearchMinQueryLength = 2;
    private int userSearchMaxQueryLength = 100;

    public String[] wsAllowedOriginPatternsArray() {
        return splitCsv(wsAllowedOriginPatterns, "*");
    }

    public String[] brokerDestinationPrefixesArray() {
        return splitCsv(brokerDestinationPrefixes, "/topic", "/queue");
    }

    private String[] splitCsv(String raw, String... defaults) {
        if (raw == null || raw.isBlank()) {
            return defaults;
        }
        String[] values = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toArray(String[]::new);
        return values.length == 0 ? defaults : values;
    }
}
