package com.hackathon.edu.dto;

public record AuthUserDto(
        String id,
        String username,
        String birthDate,
        Integer age,
        String role,
        Integer xp,
        Integer level,
        Integer coins
) {
}
