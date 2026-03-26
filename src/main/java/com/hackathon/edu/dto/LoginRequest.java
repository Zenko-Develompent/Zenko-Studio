package com.hackathon.edu.dto;

public record LoginRequest(
        String id,
        String email,
        String username,
        String password
) {
}
