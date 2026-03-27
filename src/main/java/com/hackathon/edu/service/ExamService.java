package com.hackathon.edu.service;

import com.hackathon.edu.dto.exam.ExamDTO;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.QuestEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.ExemRepository;
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
public class ExamService {
    private static final Comparator<QuestEntity> QUESTION_ORDER = Comparator
            .comparing(QuestEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(QuestEntity::getQuestId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<TasksEntity> TASK_ORDER = Comparator
            .comparing(TasksEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TasksEntity::getTasksId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ExemRepository examRepository;
    private final ProgressService progressService;

    public ExamDTO.ExamDetailResponse getExam(UUID examId) {
        ExemEntity exam = examRepository.findWithRelationsByExemId(examId)
                .orElseThrow(notFound("exam_not_found"));

        return new ExamDTO.ExamDetailResponse(
                exam.getExemId(),
                exam.getModule() == null ? null : exam.getModule().getModuleId(),
                exam.getName(),
                exam.getDescription(),
                safeInt(exam.getXpReward()),
                safeInt(exam.getCoinReward()),
                safeList(exam.getQuests()).size(),
                safeList(exam.getTasks()).size()
        );
    }

    public ExamDTO.QuestionsResponse getExamQuestions(UUID examId) {
        ExemEntity exam = examRepository.findWithRelationsByExemId(examId)
                .orElseThrow(notFound("exam_not_found"));

        List<ExamDTO.QuestionItem> items = safeList(exam.getQuests()).stream()
                .sorted(QUESTION_ORDER)
                .map(this::toQuestionItem)
                .toList();

        return new ExamDTO.QuestionsResponse(items);
    }

    public ExamDTO.TasksResponse getExamTasks(UUID examId) {
        ExemEntity exam = examRepository.findWithRelationsByExemId(examId)
                .orElseThrow(notFound("exam_not_found"));

        List<ExamDTO.TaskItem> items = safeList(exam.getTasks()).stream()
                .sorted(TASK_ORDER)
                .map(this::toTaskItem)
                .toList();

        return new ExamDTO.TasksResponse(items);
    }

    public ExamDTO.CompleteResponse completeExam(UUID userId, UUID examId) {
        ProgressService.ExamCompletionResult result = progressService.completeExam(userId, examId);
        return new ExamDTO.CompleteResponse(
                result.completed(),
                result.firstCompletion(),
                result.xpGranted(),
                result.coinGranted(),
                result.questionsDone(),
                result.questionsTotal(),
                result.tasksDone(),
                result.tasksTotal()
        );
    }

    @Transactional
    public ExamDTO.ExamDetailResponse updateExamRewards(UUID examId, ExamDTO.UpdateRewardsRequest request) {
        ExemEntity exam = examRepository.findWithRelationsByExemId(examId)
                .orElseThrow(notFound("exam_not_found"));
        exam.setXpReward(safeInt(request.xpReward()));
        exam.setCoinReward(safeInt(request.coinReward()));
        examRepository.save(exam);
        return toExamDetail(exam);
    }

    private ExamDTO.QuestionItem toQuestionItem(QuestEntity question) {
        return new ExamDTO.QuestionItem(
                question.getQuestId(),
                question.getQuiz() == null ? null : question.getQuiz().getQuizId(),
                question.getExam() == null ? null : question.getExam().getExemId(),
                question.getName(),
                question.getDescription()
        );
    }

    private ExamDTO.TaskItem toTaskItem(TasksEntity task) {
        return new ExamDTO.TaskItem(
                task.getTasksId(),
                task.getExam() == null ? null : task.getExam().getExemId(),
                task.getLesson() == null ? null : task.getLesson().getLessonId(),
                task.getName(),
                task.getDescription(),
                task.getRunnerLanguage(),
                safeInt(task.getXpReward()),
                safeInt(task.getCoinReward())
        );
    }

    private ExamDTO.ExamDetailResponse toExamDetail(ExemEntity exam) {
        return new ExamDTO.ExamDetailResponse(
                exam.getExemId(),
                exam.getModule() == null ? null : exam.getModule().getModuleId(),
                exam.getName(),
                exam.getDescription(),
                safeInt(exam.getXpReward()),
                safeInt(exam.getCoinReward()),
                safeList(exam.getQuests()).size(),
                safeList(exam.getTasks()).size()
        );
    }

    private Supplier<ApiException> notFound(String errorCode) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, errorCode);
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
