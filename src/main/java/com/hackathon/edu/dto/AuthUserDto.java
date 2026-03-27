package com.hackathon.edu.dto;

public record AuthUserDto(
        String id,
        String username,
        String location,
        String birthDate,
        Integer age,
        String role
) {
}
