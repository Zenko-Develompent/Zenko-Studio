package com.hackathon.edu.dto.exam;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public final class ExamDTO {
    private ExamDTO() {
    }

    public record ExamDetailResponse(
            UUID examId,
            UUID moduleId,
            String name,
            String description,
            Integer xpReward,
            Integer coinReward,
            long questionsCount,
            long tasksCount
    ) {
    }

    public record QuestionsResponse(
            List<QuestionItem> items
    ) {
    }

    public record QuestionItem(
            UUID questId,
            UUID quizId,
            UUID examId,
            String name,
            String description
    ) {
    }

    public record TasksResponse(
            List<TaskItem> items
    ) {
    }

    public record TaskItem(
            UUID taskId,
            UUID examId,
            UUID lessonId,
            String name,
            String description,
            Integer xpReward,
            Integer coinReward
    ) {
    }

    public record CompleteResponse(
            boolean completed,
            boolean firstCompletion,
            int xpGranted,
            int coinGranted,
            long questionsDone,
            long questionsTotal,
            long tasksDone,
            long tasksTotal
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
