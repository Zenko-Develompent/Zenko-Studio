package com.hackathon.edu.service;

import com.hackathon.edu.entity.ExamAttemptEntity;
import com.hackathon.edu.entity.ExamQuestionProgressEntity;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.QuestEntity;
import com.hackathon.edu.entity.TaskAttemptEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.ExamAttemptRepository;
import com.hackathon.edu.repository.ExamQuestionProgressRepository;
import com.hackathon.edu.repository.ExemRepository;
import com.hackathon.edu.repository.TaskAttemptRepository;
import com.hackathon.edu.repository.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ProgressService {
    private final TasksRepository tasksRepository;
    private final TaskAttemptRepository taskAttemptRepository;
    private final ExemRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionProgressRepository examQuestionProgressRepository;
    private final GamificationService gamificationService;

    @Transactional
    public TaskCompletionResult completeTask(UUID userId, UUID taskId) {
        TasksEntity task = tasksRepository.findById(taskId)
                .orElseThrow(notFound("task_not_found"));

        TaskAttemptEntity attempt = taskAttemptRepository.findByTask_TasksIdAndUserId(taskId, userId)
                .orElseGet(() -> createTaskAttempt(task, userId));

        boolean firstCompletion = !Boolean.TRUE.equals(attempt.getCompleted());
        if (firstCompletion) {
            attempt.setCompleted(true);
            attempt.setCompletedAt(OffsetDateTime.now());
            taskAttemptRepository.save(attempt);
        }

        GamificationService.GrantResult grant = GamificationService.GrantResult.none();
        if (firstCompletion && task.getLesson() != null && task.getExam() == null) {
            grant = gamificationService.grantLessonTaskReward(userId, task);
        }

        return new TaskCompletionResult(
                task.getTasksId(),
                task.getLesson() == null ? null : task.getLesson().getLessonId(),
                task.getExam() == null ? null : task.getExam().getExemId(),
                true,
                firstCompletion,
                grant.xpGranted(),
                grant.coinGranted()
        );
    }

    @Transactional
    public TaskRewardResult updateTaskRewards(UUID taskId, int xpReward, int coinReward) {
        TasksEntity task = tasksRepository.findById(taskId)
                .orElseThrow(notFound("task_not_found"));
        task.setXpReward(Math.max(0, xpReward));
        task.setCoinReward(Math.max(0, coinReward));
        tasksRepository.save(task);

        return new TaskRewardResult(
                task.getTasksId(),
                task.getLesson() == null ? null : task.getLesson().getLessonId(),
                task.getExam() == null ? null : task.getExam().getExemId(),
                safeInt(task.getXpReward()),
                safeInt(task.getCoinReward())
        );
    }

    @Transactional
    public void markExamQuestionCompleted(UUID userId, QuestEntity question) {
        if (question == null || question.getExam() == null || question.getQuestId() == null) {
            return;
        }

        boolean exists = examQuestionProgressRepository
                .findByQuestion_QuestIdAndUserId(question.getQuestId(), userId)
                .isPresent();
        if (exists) {
            return;
        }

        ExamQuestionProgressEntity progress = new ExamQuestionProgressEntity();
        progress.setQuestion(question);
        progress.setUserId(userId);
        examQuestionProgressRepository.save(progress);
    }

    @Transactional
    public ExamCompletionResult completeExam(UUID userId, UUID examId) {
        ExemEntity exam = examRepository.findWithRelationsByExemId(examId)
                .orElseThrow(notFound("exam_not_found"));

        long totalQuestions = safeList(exam.getQuests()).size();
        long totalTasks = safeList(exam.getTasks()).size();

        long doneQuestions = examQuestionProgressRepository.countByQuestion_Exam_ExemIdAndUserId(examId, userId);
        long doneTasks = taskAttemptRepository.countByTask_Exam_ExemIdAndUserIdAndCompletedTrue(examId, userId);

        if (doneQuestions < totalQuestions || doneTasks < totalTasks) {
            throw new ApiException(HttpStatus.CONFLICT, "exam_not_completed");
        }

        ExamAttemptEntity attempt = examAttemptRepository.findByExam_ExemIdAndUserId(examId, userId)
                .orElseGet(() -> createExamAttempt(exam, userId));

        boolean firstCompletion = !Boolean.TRUE.equals(attempt.getCompleted());
        if (firstCompletion) {
            attempt.setCompleted(true);
            attempt.setCompletedAt(OffsetDateTime.now());
        }

        GamificationService.GrantResult grant = GamificationService.GrantResult.none();
        if (firstCompletion && !Boolean.TRUE.equals(attempt.getRewardGranted())) {
            grant = gamificationService.grantExamReward(userId, exam);
            attempt.setRewardGranted(true);
        }

        examAttemptRepository.save(attempt);
        return new ExamCompletionResult(
                true,
                firstCompletion,
                grant.xpGranted(),
                grant.coinGranted(),
                doneQuestions,
                totalQuestions,
                doneTasks,
                totalTasks
        );
    }

    private TaskAttemptEntity createTaskAttempt(TasksEntity task, UUID userId) {
        TaskAttemptEntity created = new TaskAttemptEntity();
        created.setTask(task);
        created.setUserId(userId);
        created.setCompleted(false);
        created.setCompletedAt(null);
        return taskAttemptRepository.save(created);
    }

    private ExamAttemptEntity createExamAttempt(ExemEntity exam, UUID userId) {
        ExamAttemptEntity created = new ExamAttemptEntity();
        created.setExam(exam);
        created.setUserId(userId);
        created.setCompleted(false);
        created.setRewardGranted(false);
        created.setCompletedAt(null);
        return examAttemptRepository.save(created);
    }

    private Supplier<ApiException> notFound(String errorCode) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, errorCode);
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    public record TaskCompletionResult(
            UUID taskId,
            UUID lessonId,
            UUID examId,
            boolean completed,
            boolean firstCompletion,
            int xpGranted,
            int coinGranted
    ) {
    }

    public record TaskRewardResult(
            UUID taskId,
            UUID lessonId,
            UUID examId,
            int xpReward,
            int coinReward
    ) {
    }

    public record ExamCompletionResult(
            boolean completed,
            boolean firstCompletion,
            int xpGranted,
            int coinGranted,
            long questionsDone,
            long questionsTotal,
            long tasksDone,
            long tasksTotal
    ) {
    }
}
