package com.hackathon.edu.dto;

public record RegisterResponse(
        String userId,
        String username,
        String email,
        boolean emailVerified
) {
}
