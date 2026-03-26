package com.hackathon.edu.dto;

public record LoginRequest(
        String id,
        String username,
        String password
) {
}
