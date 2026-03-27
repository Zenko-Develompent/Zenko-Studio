package com.hackathon.edu.dto.parent;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ParentControlDTO {
    private ParentControlDTO() {
    }

    public record SendRequestRequest(
            @NotNull
            UUID childUserId
    ) {
    }

    public record RequestItem(
            UUID requestId,
            UUID parentUserId,
            String parentUsername,
            UUID childUserId,
            String childUsername,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime respondedAt
    ) {
    }

    public record RequestsResponse(
            List<RequestItem> items
    ) {
    }

    public record SendRequestResponse(
            UUID requestId,
            String status
    ) {
    }

    public record AcceptRejectResponse(
            UUID requestId,
            String status
    ) {
    }

    public record ChildItem(
            UUID childUserId,
            String childUsername,
            OffsetDateTime since
    ) {
    }

    public record ChildrenResponse(
            List<ChildItem> items
    ) {
    }

    public record ProgressItem(
            UUID targetId,
            String name,
            UUID courseId,
            UUID moduleId,
            int percent,
            boolean completed,
            long doneItems,
            long totalItems
    ) {
    }

    public record ActivityItem(
            UUID eventId,
            OffsetDateTime createdAt,
            String eventType,
            Integer progressPercent,
            Integer xpGranted,
            Integer coinGranted,
            UUID lessonId,
            UUID quizId,
            UUID taskId,
            UUID examId,
            String details
    ) {
    }

    public record ChildSummary(
            UUID userId,
            String username,
            int xp,
            int level,
            int coins,
            OffsetDateTime lastActivityAt
    ) {
    }

    public record DashboardResponse(
            ChildSummary child,
            List<ProgressItem> courses,
            List<ProgressItem> modules,
            List<ProgressItem> lessons,
            List<ActivityItem> recentActivities
    ) {
    }
}
