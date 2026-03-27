package com.hackathon.edu.service;

import com.hackathon.edu.dto.task.TaskDTO;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.TaskAttemptRepository;
import com.hackathon.edu.repository.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskRunnerService {
    private final TasksRepository tasksRepository;
    private final TaskAttemptRepository taskAttemptRepository;
    private final CodeRunnerService codeRunnerService;
    private final ProgressService progressService;
    private final LearningAccessService learningAccessService;

    @Transactional
    public TaskStartResult startTask(UUID userId, UUID taskId) {
        TasksEntity task = tasksRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "task_not_found"));

        learningAccessService.assertTaskCanStart(userId, task);
        boolean completed = taskAttemptRepository.existsByTask_TasksIdAndUserIdAndCompletedTrue(taskId, userId);

        return new TaskStartResult(
                task.getTasksId(),
                task.getLesson() == null ? null : task.getLesson().getLessonId(),
                task.getExam() == null ? null : task.getExam().getExemId(),
                completed
        );
    }

    @Transactional
    public TaskRunResult runTask(UUID userId, UUID taskId, TaskDTO.RunRequest request) {
        TasksEntity task = tasksRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "task_not_found"));
        learningAccessService.assertTaskCanStart(userId, task);

        String expectedOutput = task.getExpectedOutput();
        if (expectedOutput == null) {
            throw new ApiException(HttpStatus.CONFLICT, "task_expected_output_not_configured");
        }
        String inputData = normalizeInputData(task.getInputData());

        String language = normalizeRequiredLanguage(request.language());
        String configuredLanguage = normalizeOptionalLanguage(task.getRunnerLanguage());
        if (configuredLanguage != null && !configuredLanguage.equals(language)) {
            throw new ApiException(HttpStatus.CONFLICT, "task_language_mismatch");
        }

        CodeRunnerService.ExecutionResult execution = codeRunnerService.run(language, request.code(), inputData);
        boolean outputMatches = normalizedOutput(execution.stdout()).equals(normalizedOutput(expectedOutput));
        boolean correct = "ok".equals(execution.status()) && outputMatches;

        ProgressService.TaskCompletionResult completion = null;
        if (correct) {
            completion = progressService.completeTask(userId, taskId);
        }

        return new TaskRunResult(
                task.getTasksId(),
                language,
                execution.status(),
                correct,
                execution.stdout(),
                execution.stderr(),
                execution.exitCode(),
                execution.timedOut(),
                execution.durationMs(),
                completion != null && completion.completed(),
                completion != null && completion.firstCompletion(),
                completion == null ? 0 : completion.xpGranted(),
                completion == null ? 0 : completion.coinGranted()
        );
    }

    @Transactional
    public RunnerConfigResult updateRunnerConfig(UUID taskId, TaskDTO.UpdateRunnerRequest request) {
        TasksEntity task = tasksRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "task_not_found"));

        String language = normalizeOptionalLanguage(request.runnerLanguage());
        task.setRunnerLanguage(language);
        task.setExpectedOutput(request.expectedOutput());
        task.setInputData(normalizeInputData(request.inputData()));
        tasksRepository.save(task);

        return new RunnerConfigResult(
                task.getTasksId(),
                task.getRunnerLanguage(),
                task.getExpectedOutput() != null,
                task.getInputData() != null
        );
    }

    private String normalizeRequiredLanguage(String value) {
        String language = normalizeOptionalLanguage(value);
        if (language == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "runner_language_not_supported");
        }
        return language;
    }

    private String normalizeOptionalLanguage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if (CodeRunnerService.LANGUAGE_JAVA.equals(normalized) || CodeRunnerService.LANGUAGE_BASH.equals(normalized)) {
            return normalized;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "runner_language_not_supported");
    }

    private String normalizedOutput(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String lf = value.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = lf.split("\n", -1);
        int last = lines.length - 1;
        while (last >= 0 && lines[last].isEmpty()) {
            last--;
        }
        if (last < 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= last; i++) {
            String line = stripTrailingSpaces(lines[i]);
            sb.append(line);
            if (i < last) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String stripTrailingSpaces(String value) {
        int end = value.length();
        while (end > 0) {
            char ch = value.charAt(end - 1);
            if (ch != ' ' && ch != '\t') {
                break;
            }
            end--;
        }
        return value.substring(0, end);
    }

    private String normalizeInputData(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    public record TaskRunResult(
            UUID taskId,
            String language,
            String status,
            boolean correct,
            String stdout,
            String stderr,
            Integer exitCode,
            boolean timedOut,
            long durationMs,
            boolean completed,
            boolean firstCompletion,
            int xpGranted,
            int coinGranted
    ) {
    }

    public record TaskStartResult(
            UUID taskId,
            UUID lessonId,
            UUID examId,
            boolean completed
    ) {
    }

    public record RunnerConfigResult(
            UUID taskId,
            String runnerLanguage,
            boolean hasExpectedOutput,
            boolean hasInputData
    ) {
    }
}
