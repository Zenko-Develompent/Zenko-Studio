package com.hackathon.edu.dto;

import java.util.List;
import java.util.UUID;

public final class ProfileDTO {
    private ProfileDTO() {
    }

    public record PrivateProfileResponse(
            String userId,
            String username,
            String role,
            int xp,
            int level,
            int coins,
            List<AchievementItem> achievements
    ) {
    }

    public record PublicProfileResponse(
            String userId,
            String username,
            String role,
            int level,
            int exp,
            List<AchievementItem> achievements
    ) {
    }

    public record AchievementItem(
            UUID achievementId,
            String name,
            String description,
            String icon
    ) {
    }
}

