package com.hackathon.edu.dto.course;

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
            UUID examId
    ) {
    }

    public record CourseTreeResponse(
            UUID courseId,
            String name,
            List<CourseTreeModuleItem> modules
    ) {
    }

    public record CourseTreeModuleItem(
            UUID moduleId,
            String name,
            UUID examId,
            List<CourseTreeLessonItem> lessons
    ) {
    }

    public record CourseTreeLessonItem(
            UUID lessonId,
            String name,
            String body,
            UUID quizId,
            UUID taskId
    ) {
    }
}

