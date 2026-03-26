package com.hackathon.edu.dto.exam;

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
            String description
    ) {
    }
}

