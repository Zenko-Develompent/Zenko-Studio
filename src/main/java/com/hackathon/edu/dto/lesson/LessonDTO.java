package com.hackathon.edu.dto.lesson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class LessonDTO {
    private LessonDTO() {
    }

    public record LessonDetailResponse(
            UUID lessonId,
            String name,
            String description,
            String content,
            Integer xp,
            UUID quizId,
            UUID taskId
    ) {
    }

    public record LessonQuizResponse(
            UUID quizId,
            UUID lessonId,
            String name,
            String description,
            long questionsCount
    ) {
    }

    public record LessonTaskResponse(
            UUID taskId,
            UUID lessonId,
            UUID examId,
            String name,
            String description,
            Integer xpReward,
            Integer coinReward
    ) {
    }

    public record LessonUpdateRequest(
            @NotBlank
            @Size(max = 50)
            String name,
            String description,
            Integer xp
    ) {
    }

    public record LessonBodyLinkRequest(
            @NotBlank
            String body
    ) {
    }
}
