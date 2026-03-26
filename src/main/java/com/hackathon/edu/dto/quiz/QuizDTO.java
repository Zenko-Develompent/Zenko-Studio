package com.hackathon.edu.dto.quiz;

import java.util.List;
import java.util.UUID;

public final class QuizDTO {
    private QuizDTO() {
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

