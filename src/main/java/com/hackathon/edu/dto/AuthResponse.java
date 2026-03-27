package com.hackathon.edu.dto;
//zzzz
public record AuthResponse(
        String accessToken,
        String accessExpiresAt,
        String refreshToken,
        String refreshExpiresAt,
        AuthUserDto user
) {
}
