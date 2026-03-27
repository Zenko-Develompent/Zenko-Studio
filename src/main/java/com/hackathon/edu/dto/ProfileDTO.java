package com.hackathon.edu.dto;

import java.util.List;
import java.util.UUID;

public final class ProfileDTO {
    private ProfileDTO() {
    }

    public record ProfileResponse(
            String username,
            int level,
            List<AchievementItem> achievements
    ) {
    }

    public record AchievementItem(
            UUID achievementId,
            String name
    ) {
    }
}

