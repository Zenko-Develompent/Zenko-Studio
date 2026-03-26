package com.hackathon.edu.service;

import com.hackathon.edu.dto.quiz.QuizDTO;
import com.hackathon.edu.entity.QuestEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {
    private static final Comparator<QuestEntity> QUESTION_ORDER = Comparator
            .comparing(QuestEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(QuestEntity::getQuestId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final QuizRepository quizRepository;

    public QuizDTO.QuestionsResponse getQuizQuestions(UUID quizId) {
        QuizEntity quiz = quizRepository.findWithQuestsByQuizId(quizId)
                .orElseThrow(notFound("quiz_not_found"));

        List<QuizDTO.QuestionItem> items = safeList(quiz.getQuests()).stream()
                .sorted(QUESTION_ORDER)
                .map(this::toQuestionItem)
                .toList();

        return new QuizDTO.QuestionsResponse(items);
    }

    private QuizDTO.QuestionItem toQuestionItem(QuestEntity question) {
        return new QuizDTO.QuestionItem(
                question.getQuestId(),
                question.getQuiz() == null ? null : question.getQuiz().getQuizId(),
                question.getExam() == null ? null : question.getExam().getExemId(),
                question.getName(),
                question.getDescription()
        );
    }

    private Supplier<ApiException> notFound(String errorCode) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, errorCode);
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }
}

