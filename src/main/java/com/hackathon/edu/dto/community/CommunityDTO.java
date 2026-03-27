package com.hackathon.edu.dto.community;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class CommunityDTO {
    private CommunityDTO() {
    }

    public record LeaderboardResponse(
            String period,
            String metric,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive,
            List<LeaderboardItem> items
    ) {
    }

    public record LeaderboardItem(
            int rank,
            UUID userId,
            String username,
            long score
    ) {
    }

    public record FeedResponse(
            List<FeedItem> items
    ) {
    }

    public record FeedItem(
            UUID eventId,
            OffsetDateTime createdAt,
            UUID userId,
            String username,
            String eventType,
            int activityScore,
            int xpGranted,
            int coinGranted,
            Integer progressPercent,
            UUID lessonId,
            UUID quizId,
            UUID taskId,
            UUID examId,
            String details
    ) {
    }
}
