package com.hackathon.edu.dto.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class TaskDTO {
    private TaskDTO() {
    }

    public record CompleteResponse(
            UUID taskId,
            UUID lessonId,
            UUID examId,
            boolean completed,
            boolean firstCompletion,
            int xpGranted,
            int coinGranted
    ) {
    }

    public record RewardResponse(
            UUID taskId,
            UUID lessonId,
            UUID examId,
            int xpReward,
            int coinReward
    ) {
    }

    public record UpdateRewardsRequest(
            @NotNull
            @Min(0)
            Integer xpReward,
            @NotNull
            @Min(0)
            Integer coinReward
    ) {
    }
}
