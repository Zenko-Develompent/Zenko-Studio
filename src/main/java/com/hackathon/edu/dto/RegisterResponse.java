package com.hackathon.edu.dto;

public record RegisterResponse(
        String userId,
        String username,
        Integer age,
        String role
) {
}
