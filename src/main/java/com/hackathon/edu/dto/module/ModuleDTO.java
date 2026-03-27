package com.hackathon.edu.dto.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public record ModuleListResponse(
            List<ModuleListItem> items
    ) {
    }

    public record ModuleListItem(
            UUID moduleId,
            UUID courseId,
            String name,
            String description,
            long lessonCount,
            UUID examId
    ) {
    }

    public record ModuleCreateRequest(
            UUID courseId,
            @NotBlank
            @Size(max = 50)
            String name,
            String description,
            List<UUID> lessonIds
    ) {
    }

    public record ModuleUpdateRequest(
            UUID courseId,
            @NotBlank
            @Size(max = 50)
            String name,
            String description,
            List<UUID> lessonIds
    ) {
    }
}
