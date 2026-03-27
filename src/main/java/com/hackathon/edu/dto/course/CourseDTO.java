package com.hackathon.edu.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class CourseDTO {
    private CourseDTO() {
    }

    public record CourseListResponse(
            List<CourseListItem> items,
            int page,
            int size,
            long total
    ) {
    }

    public record CourseListItem(
            UUID courseId,
            String name,
            String description,
            String category,
            long moduleCount,
            long lessonCount
    ) {
    }

    public record CourseDetailResponse(
            UUID courseId,
            String name,
            String description,
            String category,
            List<CourseModuleItem> modules
    ) {
    }

    public record CourseModulesResponse(
            List<CourseModuleItem> items
    ) {
    }

    public record CourseModuleItem(
            UUID moduleId,
            String name,
            String description,
            long lessonCount,
            UUID examId,
            Boolean unlocked
    ) {
    }

    public record CourseTreeResponse(
            UUID courseId,
            String name,
            String description,
            List<CourseTreeModuleItem> modules
    ) {
    }

    public record CourseTreeModuleItem(
            UUID moduleId,
            String name,
            UUID examId,
            Boolean unlocked,
            List<CourseTreeLessonItem> lessons
    ) {
    }

    public record CourseTreeLessonItem(
            UUID lessonId,
            String name,
            UUID quizId,
            UUID taskId,
            Boolean unlocked
    ) {
    }

    public record CreateCourseRequest(
            //@NotNull UUID courseId,
            @NotBlank @Size(max = 50) String name,
            String description,
            String category,
            List<ModuleCreateRequest> modules
            //@NotNull OffsetDateTime createdAt,
            //@NotNull OffsetDateTime updatedAt
    ) {}

    public record ModuleCreateRequest(
            @NotBlank String name,
            String description
    ) {}
}

