package com.hackathon.edu.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class QuizDTO {
    private QuizDTO() {
    }

    public record QuizDetailResponse(
            UUID quizId,
            UUID lessonId,
            String name,
            String description,
            Integer xpReward,
            Integer coinReward,
            List<QuestionDetailItem> questions
    ) {
    }

    public record QuestionDetailItem(
            UUID questId,
            String name,
            String description,
            List<AnswerItem> answers
    ) {
    }

    public record AnswerItem(
            UUID answerId,
            String name,
            String description
    ) {
    }

    public record AnswersResponse(
            List<AnswerItem> items
    ) {
    }

    public record QuizCreateRequest(
            @NotBlank
            @Size(max = 50)
            String name,
            String description,
            Integer xpReward,
            Integer coinReward,
            @NotEmpty
            List<QuestionCreateRequest> questions
    ) {
    }

    public record QuestionCreateRequest(
            @NotBlank
            @Size(max = 50)
            String name,
            String description,
            @NotEmpty
            List<AnswerCreateRequest> answers
    ) {
    }

    public record AnswerCreateRequest(
            @NotBlank
            @Size(max = 50)
            String name,
            String description,
            @NotNull
            Boolean correct
    ) {
    }

    public record CheckAnswerRequest(
            @NotNull
            UUID answerId
    ) {
    }

    public record CheckAnswerResponse(
            boolean correct
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
}

