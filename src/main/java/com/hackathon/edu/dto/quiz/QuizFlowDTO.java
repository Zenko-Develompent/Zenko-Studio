package com.hackathon.edu.dto.quiz;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public final class QuizFlowDTO {
    private QuizFlowDTO() {
    }

    public record StartResponse(
            boolean completed,
            QuestionItem question
    ) {
    }

    public record SubmitAnswerRequest(
            @NotNull
            UUID questionId,
            @NotNull
            UUID answerId
    ) {
    }

    public record SubmitAnswerResponse(
            boolean correct,
            boolean completed,
            int xpGranted,
            int coinGranted,
            QuestionItem question
    ) {
    }

    public record QuestionItem(
            UUID questionId,
            String name,
            String description,
            int index,
            int total,
            List<AnswerOption> options
    ) {
    }

    public record AnswerOption(
            UUID answerId,
            String name,
            String description
    ) {
    }

}
