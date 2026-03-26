package com.hackathon.edu.dto.module;

import java.util.List;
import java.util.UUID;

public final class ModuleDTO {
    private ModuleDTO() {
    }

    public record ModuleDetailResponse(
            UUID moduleId,
            UUID courseId,
            String name,
            String description,
            ModuleExamSummary exam
    ) {
    }

    public record ModuleExamSummary(
            UUID examId,
            String name,
            long questionsCount,
            long tasksCount
    ) {
    }

    public record ModuleLessonsResponse(
            List<LessonCard> items
    ) {
    }

    public record LessonCard(
            UUID lessonId,
            String name,
            String description,
            String body,
            Integer xp,
            UUID quizId,
            UUID taskId
    ) {
    }

    public record ModuleExamResponse(
            UUID examId,
            UUID moduleId,
            String name,
            String description,
            long questionsCount,
            long tasksCount
    ) {
    }
}

