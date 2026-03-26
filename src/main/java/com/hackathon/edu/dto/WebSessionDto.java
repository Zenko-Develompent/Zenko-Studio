package com.hackathon.edu.dto;

public record WebSessionDto(
        String sessionId,
        String deviceId,
        String userAgent,
        String ip,
        String createdAt,
        String lastActivityAt,
        String expiresAt,
        boolean current
) {
}
