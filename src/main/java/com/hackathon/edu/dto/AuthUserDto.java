package com.hackathon.edu.dto;

public record AuthUserDto(
        String id,
        String username,
        String email,
        Boolean emailVerified,
        String displayName,
        String bio,
        String location,
        String website,
        String birthDate,
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
