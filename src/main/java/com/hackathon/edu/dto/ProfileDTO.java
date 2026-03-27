package com.hackathon.edu.dto;

import java.util.List;
import java.util.UUID;

public final class ProfileDTO {
    private ProfileDTO() {
    }

    public record PrivateProfileResponse(
            String username,
            int xp,
            int level,
            int coins,
            List<AchievementItem> achievements
    ) {
    }

    public record PublicProfileResponse(
            String username,
            int level,
            int exp,
            List<AchievementItem> achievements
    ) {
    }

    public record AchievementItem(
            UUID achievementId,
            String name,
            String icon
    ) {
    }
}

