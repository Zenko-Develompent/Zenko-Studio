package com.hackathon.edu.dto;

public record RegisterRequest(
        String username,
        String email,
        String password
) {
}
