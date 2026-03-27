package com.hackathon.edu.service;

import com.hackathon.edu.dto.quiz.QuizFlowDTO;
import com.hackathon.edu.dto.quiz.QuizDTO;
import com.hackathon.edu.entity.AnswerEntity;
import com.hackathon.edu.entity.QuestEntity;
import com.hackathon.edu.entity.QuizAttemptEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.QuizAttemptRepository;
import com.hackathon.edu.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
    private static final Comparator<AnswerEntity> ANSWER_ORDER = Comparator
            .comparing(AnswerEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AnswerEntity::getAnswerId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public QuizDTO.QuestionsResponse getQuizQuestions(UUID quizId) {
        QuizEntity quiz = quizRepository.findWithQuestsByQuizId(quizId)
                .orElseThrow(notFound("quiz_not_found"));

        List<QuizDTO.QuestionItem> items = safeList(quiz.getQuests()).stream()
                .sorted(QUESTION_ORDER)
                .map(this::toQuestionItem)
                .toList();

        return new QuizDTO.QuestionsResponse(items);
    }

    @Transactional
    public QuizFlowDTO.StartResponse startQuiz(UUID userId, UUID quizId) {
        QuizEntity quiz = quizRepository.findWithFlowByQuizId(quizId)
                .orElseThrow(notFound("quiz_not_found"));
        QuizAttemptEntity attempt = getOrCreateAttempt(userId, quiz);
        List<QuestEntity> questions = orderedQuestions(quiz);
        if (questions.isEmpty() || isCompleted(attempt, questions.size())) {
            markCompleted(attempt);
            return new QuizFlowDTO.StartResponse(true, null, lessonTask(quiz));
        }

        int currentIndex = safeIndex(attempt);
        return new QuizFlowDTO.StartResponse(
                false,
                toFlowQuestion(questions.get(currentIndex), currentIndex, questions.size()),
                null
        );
    }

    @Transactional
    public QuizFlowDTO.SubmitAnswerResponse submitAnswer(UUID userId, UUID quizId, QuizFlowDTO.SubmitAnswerRequest request) {
        QuizEntity quiz = quizRepository.findWithFlowByQuizId(quizId)
                .orElseThrow(notFound("quiz_not_found"));
        QuizAttemptEntity attempt = getOrCreateAttempt(userId, quiz);
        List<QuestEntity> questions = orderedQuestions(quiz);
        if (questions.isEmpty() || isCompleted(attempt, questions.size())) {
            markCompleted(attempt);
            return new QuizFlowDTO.SubmitAnswerResponse(true, true, null, lessonTask(quiz));
        }

        int questionIndex = safeIndex(attempt);
        QuestEntity current = questions.get(questionIndex);
        if (!current.getQuestId().equals(request.questionId())) {
            throw new ApiException(HttpStatus.CONFLICT, "quiz_question_out_of_order");
        }

        AnswerEntity answer = findAnswer(current, request.answerId());
        if (answer == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "answer_not_in_question");
        }

        boolean isCorrect = Boolean.TRUE.equals(answer.getCorrectly());
        if (!isCorrect) {
            return new QuizFlowDTO.SubmitAnswerResponse(
                    false,
                    false,
                    toFlowQuestion(current, questionIndex, questions.size()),
                    null
            );
        }

        int nextIndex = questionIndex + 1;
        attempt.setCurrentQuestionIndex(nextIndex);
        if (nextIndex < questions.size()) {
            quizAttemptRepository.save(attempt);
            return new QuizFlowDTO.SubmitAnswerResponse(
                    true,
                    false,
                    toFlowQuestion(questions.get(nextIndex), nextIndex, questions.size()),
                    null
            );
        }

        markCompleted(attempt);
        return new QuizFlowDTO.SubmitAnswerResponse(true, true, null, lessonTask(quiz));
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

    private QuizFlowDTO.QuestionItem toFlowQuestion(QuestEntity question, int index, int total) {
        List<QuizFlowDTO.AnswerOption> options = safeList(question.getAnswers()).stream()
                .sorted(ANSWER_ORDER)
                .map(answer -> new QuizFlowDTO.AnswerOption(
                        answer.getAnswerId(),
                        answer.getName(),
                        answer.getDescription()
                ))
                .toList();

        return new QuizFlowDTO.QuestionItem(
                question.getQuestId(),
                question.getName(),
                question.getDescription(),
                index + 1,
                total,
                options
        );
    }

    private QuizFlowDTO.TaskItem lessonTask(QuizEntity quiz) {
        TasksEntity task = quiz.getLesson() == null ? null : quiz.getLesson().getTask();
        if (task == null) {
            return null;
        }
        return new QuizFlowDTO.TaskItem(
                task.getTasksId(),
                task.getLesson() == null ? null : task.getLesson().getLessonId(),
                task.getExam() == null ? null : task.getExam().getExemId(),
                task.getName(),
                task.getDescription()
        );
    }

    private AnswerEntity findAnswer(QuestEntity question, UUID answerId) {
        return safeList(question.getAnswers()).stream()
                .filter(answer -> answerId.equals(answer.getAnswerId()))
                .findFirst()
                .orElse(null);
    }

    private List<QuestEntity> orderedQuestions(QuizEntity quiz) {
        return safeList(quiz.getQuests()).stream()
                .sorted(QUESTION_ORDER)
                .toList();
    }

    private QuizAttemptEntity getOrCreateAttempt(UUID userId, QuizEntity quiz) {
        return quizAttemptRepository.findByQuiz_QuizIdAndUserId(quiz.getQuizId(), userId)
                .orElseGet(() -> createAttempt(userId, quiz));
    }

    private QuizAttemptEntity createAttempt(UUID userId, QuizEntity quiz) {
        QuizAttemptEntity created = new QuizAttemptEntity();
        created.setQuiz(quiz);
        created.setUserId(userId);
        created.setCurrentQuestionIndex(0);
        created.setCompleted(false);
        created.setCompletedAt(null);
        try {
            return quizAttemptRepository.saveAndFlush(created);
        } catch (DataIntegrityViolationException ex) {
            return quizAttemptRepository.findByQuiz_QuizIdAndUserId(quiz.getQuizId(), userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));
        }
    }

    private void markCompleted(QuizAttemptEntity attempt) {
        if (!Boolean.TRUE.equals(attempt.getCompleted())) {
            attempt.setCompleted(true);
            attempt.setCompletedAt(OffsetDateTime.now());
            quizAttemptRepository.save(attempt);
        }
    }

    private boolean isCompleted(QuizAttemptEntity attempt, int totalQuestions) {
        return Boolean.TRUE.equals(attempt.getCompleted()) || safeIndex(attempt) >= totalQuestions;
    }

    private int safeIndex(QuizAttemptEntity attempt) {
        Integer current = attempt.getCurrentQuestionIndex();
        return current == null || current < 0 ? 0 : current;
    }

    private Supplier<ApiException> notFound(String errorCode) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, errorCode);
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }
}
