package com.hackathon.edu.service;

import com.hackathon.edu.dto.progress.ProgressDTO;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.CourseRepository;
import com.hackathon.edu.repository.ExamAttemptRepository;
import com.hackathon.edu.repository.LessonRepository;
import com.hackathon.edu.repository.ModuleRepository;
import com.hackathon.edu.repository.QuizAttemptRepository;
import com.hackathon.edu.repository.TaskAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressQueryService {
    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final TaskAttemptRepository taskAttemptRepository;
    private final ExamAttemptRepository examAttemptRepository;

    public ProgressDTO.ProgressResponse getLessonProgress(UUID userId, UUID lessonId) {
        LessonEntity lesson = lessonRepository.findWithRelationsByLessonId(lessonId)
                .orElseThrow(notFound("lesson_not_found"));

        ProgressValue value = calculateLessonProgress(userId, lesson);
        return value.toResponse(lessonId, "lesson");
    }

    public ProgressDTO.ProgressResponse getModuleProgress(UUID userId, UUID moduleId) {
        ModuleEntity moduleWithLessons = moduleRepository.findWithLessonsByModuleId(moduleId)
                .orElseThrow(notFound("module_not_found"));
        ModuleEntity moduleWithExam = moduleRepository.findWithCourseAndExamByModuleId(moduleId)
                .orElseThrow(notFound("module_not_found"));

        ProgressValue value = calculateModuleProgress(userId, moduleWithLessons.getLessons(), moduleWithExam.getExam());
        return value.toResponse(moduleId, "module");
    }

    public ProgressDTO.ProgressResponse getCourseProgress(UUID userId, UUID courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "course_not_found");
        }

        List<ModuleEntity> modules = moduleRepository.findByCourse_CourseIdOrderByNameAsc(courseId);
        long total = 0;
        long done = 0;

        for (ModuleEntity module : modules) {
            ModuleEntity moduleWithLessons = moduleRepository.findWithLessonsByModuleId(module.getModuleId())
                    .orElseThrow(notFound("module_not_found"));
            ProgressValue moduleProgress = calculateModuleProgress(userId, moduleWithLessons.getLessons(), module.getExam());
            done += moduleProgress.doneItems();
            total += moduleProgress.totalItems();
        }

        return ProgressValue.of(done, total).toResponse(courseId, "course");
    }

    private ProgressValue calculateModuleProgress(UUID userId, List<LessonEntity> lessons, ExemEntity exam) {
        long total = 0;
        long done = 0;

        for (LessonEntity lesson : safeList(lessons)) {
            ProgressValue lessonProgress = calculateLessonProgress(userId, lesson);
            done += lessonProgress.doneItems();
            total += lessonProgress.totalItems();
        }

        if (exam != null && exam.getExemId() != null) {
            total++;
            if (examAttemptRepository.existsByExam_ExemIdAndUserIdAndCompletedTrue(exam.getExemId(), userId)) {
                done++;
            }
        }

        return ProgressValue.of(done, total);
    }

    private ProgressValue calculateLessonProgress(UUID userId, LessonEntity lesson) {
        long total = 0;
        long done = 0;

        QuizEntity quiz = lesson.getQuiz();
        if (quiz != null && quiz.getQuizId() != null) {
            total++;
            if (quizAttemptRepository.existsByQuiz_QuizIdAndUserIdAndCompletedTrue(quiz.getQuizId(), userId)) {
                done++;
            }
        }

        TasksEntity task = lesson.getTask();
        if (task != null && task.getTasksId() != null) {
            total++;
            if (taskAttemptRepository.existsByTask_TasksIdAndUserIdAndCompletedTrue(task.getTasksId(), userId)) {
                done++;
            }
        }

        return ProgressValue.of(done, total);
    }

    private Supplier<ApiException> notFound(String errorCode) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, errorCode);
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static int toPercent(long done, long total) {
        if (total <= 0) {
            return 100;
        }
        int percent = (int) Math.round((done * 100.0d) / total);
        if (percent < 0) {
            return 0;
        }
        return Math.min(percent, 100);
    }

    private record ProgressValue(long doneItems, long totalItems) {
        static ProgressValue of(long doneItems, long totalItems) {
            return new ProgressValue(Math.max(0, doneItems), Math.max(0, totalItems));
        }

        boolean completed() {
            return totalItems <= 0 || doneItems >= totalItems;
        }

        int percent() {
            return toPercent(doneItems, totalItems);
        }

        ProgressDTO.ProgressResponse toResponse(UUID targetId, String targetType) {
            return new ProgressDTO.ProgressResponse(
                    targetId,
                    targetType,
                    percent(),
                    completed(),
                    doneItems,
                    totalItems
            );
        }
    }
}
