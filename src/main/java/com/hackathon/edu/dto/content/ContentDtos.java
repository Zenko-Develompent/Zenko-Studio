package com.hackathon.edu.dto.content;

import java.util.List;
import java.util.UUID;

public final class ContentDtos {
    private ContentDtos() {
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

    public record ExamDetailResponse(
            UUID examId,
            UUID moduleId,
            String name,
            String description,
            long questionsCount,
            long tasksCount
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
            String description
    ) {
    }
}

