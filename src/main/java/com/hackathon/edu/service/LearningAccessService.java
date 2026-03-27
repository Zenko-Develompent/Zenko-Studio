package com.hackathon.edu.service;

import com.hackathon.edu.dto.progress.ProgressDTO;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.ModuleRepository;
import com.hackathon.edu.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LearningAccessService {
    private static final Comparator<ModuleEntity> MODULE_ORDER = Comparator
            .comparing(ModuleEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(ModuleEntity::getModuleId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<LessonEntity> LESSON_ORDER = Comparator
            .comparing(LessonEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(LessonEntity::getLessonId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ModuleRepository moduleRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ProgressQueryService progressQueryService;

    public void assertLessonQuizCanStart(UUID userId, QuizEntity quiz) {
        if (quiz == null || quiz.getLesson() == null || quiz.getLesson().getLessonId() == null) {
            return;
        }
        assertLessonUnlocked(userId, quiz.getLesson().getLessonId());
    }

    public void assertTaskCanStart(UUID userId, TasksEntity task) {
        if (task == null) {
            return;
        }

        if (task.getLesson() != null && task.getLesson().getLessonId() != null) {
            UUID lessonId = task.getLesson().getLessonId();
            assertLessonUnlocked(userId, lessonId);

            QuizEntity lessonQuiz = task.getLesson().getQuiz();
            if (lessonQuiz != null
                    && lessonQuiz.getQuizId() != null
                    && !quizAttemptRepository.existsByQuiz_QuizIdAndUserIdAndCompletedTrue(lessonQuiz.getQuizId(), userId)) {
                throw new ApiException(HttpStatus.CONFLICT, "lesson_quiz_not_completed");
            }
            return;
        }

        if (task.getExam() != null && task.getExam().getExemId() != null) {
            assertExamUnlocked(userId, task.getExam());
        }
    }

    public void assertExamUnlocked(UUID userId, ExemEntity exam) {
        if (exam == null || exam.getExemId() == null) {
            return;
        }
        ModuleEntity module = moduleRepository.findWithCourseExamAndLessonsByExam_ExemId(exam.getExemId())
                .orElse(null);
        if (module == null) {
            return;
        }

        assertModuleUnlocked(userId, module);

        List<LessonEntity> orderedLessons = safeList(module.getLessons()).stream()
                .sorted(LESSON_ORDER)
                .toList();

        for (LessonEntity lesson : orderedLessons) {
            if (lesson == null || lesson.getLessonId() == null) {
                continue;
            }
            ProgressDTO.ProgressResponse lessonProgress = progressQueryService.getLessonProgress(userId, lesson.getLessonId());
            if (!lessonProgress.completed()) {
                throw new ApiException(HttpStatus.CONFLICT, "exam_locked");
            }
        }
    }

    public void assertLessonUnlocked(UUID userId, UUID lessonId) {
        ModuleEntity module = moduleRepository.findWithCourseExamAndLessonsByLessonId(lessonId)
                .orElse(null);
        if (module == null) {
            return;
        }

        assertModuleUnlocked(userId, module);

        List<LessonEntity> orderedLessons = safeList(module.getLessons()).stream()
                .sorted(LESSON_ORDER)
                .toList();

        int currentIndex = findLessonIndex(orderedLessons, lessonId);
        if (currentIndex <= 0) {
            return;
        }

        LessonEntity previous = orderedLessons.get(currentIndex - 1);
        if (previous == null || previous.getLessonId() == null) {
            return;
        }

        ProgressDTO.ProgressResponse previousProgress = progressQueryService.getLessonProgress(userId, previous.getLessonId());
        if (!previousProgress.completed()) {
            throw new ApiException(HttpStatus.CONFLICT, "lesson_locked");
        }
    }

    public boolean isModuleUnlocked(UUID userId, UUID moduleId) {
        ModuleEntity module = moduleRepository.findWithCourseAndExamByModuleId(moduleId)
                .orElse(null);
        if (module == null) {
            return true;
        }
        try {
            assertModuleUnlocked(userId, module);
            return true;
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.CONFLICT && "module_locked".equals(ex.getErrorCode())) {
                return false;
            }
            throw ex;
        }
    }

    public boolean isLessonUnlocked(UUID userId, UUID lessonId) {
        try {
            assertLessonUnlocked(userId, lessonId);
            return true;
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.CONFLICT) {
                String code = ex.getErrorCode();
                if ("module_locked".equals(code) || "lesson_locked".equals(code)) {
                    return false;
                }
            }
            throw ex;
        }
    }

    private void assertModuleUnlocked(UUID userId, ModuleEntity module) {
        if (module.getCourse() == null || module.getCourse().getCourseId() == null || module.getModuleId() == null) {
            return;
        }

        List<ModuleEntity> orderedModules = moduleRepository.findByCourse_CourseIdOrderByNameAsc(module.getCourse().getCourseId())
                .stream()
                .sorted(MODULE_ORDER)
                .toList();

        int currentIndex = findModuleIndex(orderedModules, module.getModuleId());
        if (currentIndex <= 0) {
            return;
        }

        ModuleEntity previous = orderedModules.get(currentIndex - 1);
        if (previous == null || previous.getModuleId() == null) {
            return;
        }

        ProgressDTO.ProgressResponse previousProgress = progressQueryService.getModuleProgress(userId, previous.getModuleId());
        if (!previousProgress.completed()) {
            throw new ApiException(HttpStatus.CONFLICT, "module_locked");
        }
    }

    private int findModuleIndex(List<ModuleEntity> modules, UUID moduleId) {
        for (int i = 0; i < modules.size(); i++) {
            ModuleEntity module = modules.get(i);
            if (module != null && moduleId.equals(module.getModuleId())) {
                return i;
            }
        }
        return -1;
    }

    private int findLessonIndex(List<LessonEntity> lessons, UUID lessonId) {
        for (int i = 0; i < lessons.size(); i++) {
            LessonEntity lesson = lessons.get(i);
            if (lesson != null && lessonId.equals(lesson.getLessonId())) {
                return i;
            }
        }
        return -1;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
