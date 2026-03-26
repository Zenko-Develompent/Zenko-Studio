package com.hackathon.edu.dto.lesson;

import java.util.UUID;

public final class LessonDTO {
    private LessonDTO() {
    }

    public record LessonDetailResponse(
            UUID lessonId,
            UUID moduleId,
            String name,
            String description,
            String body,
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
            String description
    ) {
    }
}

