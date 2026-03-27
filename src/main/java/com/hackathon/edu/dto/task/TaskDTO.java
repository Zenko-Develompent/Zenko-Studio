package com.hackathon.edu.dto.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    public record RunRequest(
            @NotBlank
            @Size(max = 16)
            String language,
            @NotBlank
            @Size(max = 20000)
            String code
    ) {
    }

    public record RunResponse(
            UUID taskId,
            String language,
            String status,
            boolean correct,
            String stdout,
            String stderr,
            Integer exitCode,
            boolean timedOut,
            long durationMs,
            boolean completed,
            boolean firstCompletion,
            int xpGranted,
            int coinGranted
    ) {
    }

    public record RunnerConfigResponse(
            UUID taskId,
            String runnerLanguage,
            boolean hasExpectedOutput,
            boolean hasInputData
    ) {
    }

    public record UpdateRunnerRequest(
            @Size(max = 16)
            String runnerLanguage,
            @NotNull
            @Size(max = 10000)
            String expectedOutput,
            @Size(max = 4000)
            String inputData
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
