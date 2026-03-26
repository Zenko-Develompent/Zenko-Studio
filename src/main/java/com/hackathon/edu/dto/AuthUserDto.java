package com.hackathon.edu.dto;

public record AuthUserDto(
        String id,
        String username,
        String displayName,
        String bio,
        String location,
        String website,
        String birthDate,
        Integer age,
        String role,
        String avatarUrl,
        String bannerUrl,
        String profileWallpaper,
        String profileTheme,
        String customThemeColor1,
        String customThemeColor2,
        String userStatus,
        String customStatus,
        Object socialLinks
) {
}
