package com.hackathon.edu.dto;
//zzz
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
