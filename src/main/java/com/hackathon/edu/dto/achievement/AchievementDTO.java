package com.hackathon.edu.dto.achievement;

import java.util.List;

public final class AchievementDTO {
    private AchievementDTO() {
    }

    public record AchievementItem(
            String code,
            String name,
            String description,
            String icon,
            int order,
            boolean unlocked
    ) {
    }

    public record AchievementListResponse(
            List<AchievementItem> items
    ) {
    }
}
