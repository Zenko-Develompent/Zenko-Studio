package com.hackathon.edu.dto;

public record AuthResponse(
        String accessToken,
        String accessExpiresAt,
        String refreshToken,
        String refreshExpiresAt,
        AuthUserDto user
) {
}
