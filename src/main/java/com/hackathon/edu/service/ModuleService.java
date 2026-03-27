package com.hackathon.edu.service;

import com.hackathon.edu.dto.module.ModuleDTO;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.CourseRepository;
import com.hackathon.edu.repository.ExemRepository;
import com.hackathon.edu.repository.LessonRepository;
import com.hackathon.edu.repository.ModuleRepository;
import com.hackathon.edu.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModuleService {
    private static final Comparator<ModuleEntity> MODULE_ORDER = Comparator
            .comparing(ModuleEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(ModuleEntity::getModuleId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<LessonEntity> LESSON_ORDER = Comparator
            .comparing(LessonEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(LessonEntity::getLessonId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final ExemRepository examRepository;
    private final ProgressQueryService progressQueryService;
    private final LearningAccessService learningAccessService;

    @Transactional
    public ModuleDTO.ModuleDetailResponse createModule(ModuleDTO.ModuleCreateRequest request) {
        ModuleEntity module = new ModuleEntity();
        if (request.courseId() != null) {
            module.setCourse(courseRepository.findById(request.courseId())
                    .orElseThrow(notFound("course_not_found")));
        }
        module.setName(request.name());
        module.setDescription(request.description());

        module = moduleRepository.save(module);

        attachLessonsToModule(module, request.lessonIds());

        return toDetailResponse(module);
    }

    @Transactional
    public ModuleDTO.ModuleDetailResponse updateModule(UUID moduleId, ModuleDTO.ModuleUpdateRequest request) {
        ModuleEntity module = moduleRepository.findWithCourseAndExamByModuleId(moduleId)
                .orElseThrow(notFound("module_not_found"));

        if (request.courseId() != null) {
            module.setCourse(courseRepository.findById(request.courseId())
                    .orElseThrow(notFound("course_not_found")));
        }
        module.setName(request.name());
        module.setDescription(request.description());

        attachLessonsToModule(module, request.lessonIds());

        return toDetailResponse(module);
    }

    @Transactional
    public void deleteModule(UUID moduleId) {
        ModuleEntity module = moduleRepository.findWithLessonsByModuleId(moduleId)
                .orElseThrow(notFound("module_not_found"));

        
        List<QuizEntity> quizzes = safeList(module.getLessons()).stream()
                .map(LessonEntity::getQuiz)
                .filter(q -> q != null && q.getQuizId() != null)
                .toList();
        quizRepository.deleteAll(quizzes);

        
        safeList(module.getLessons()).stream()
                .map(LessonEntity::getTask)
                .filter(t -> t != null && t.getTasksId() != null)
                .forEach(task -> task.setLesson(null));

        moduleRepository.delete(module);
    }

    public ModuleDTO.ModuleListResponse listModulesByCourse(UUID courseId) {
        return listModulesByCourse(courseId, null);
    }

    public ModuleDTO.ModuleListResponse listModulesByCourse(UUID courseId, UUID userId) {
        if (!courseRepository.existsById(courseId)) {
            throw notFound("course_not_found").get();
        }

        Map<UUID, Long> lessonCounts = lessonRepository.countLessonsByModuleForCourse(courseId).stream()
                .collect(Collectors.toMap(LessonRepository.ModuleLessonCount::getModuleId, LessonRepository.ModuleLessonCount::getCnt));

        List<ModuleEntity> modules = moduleRepository.findByCourse_CourseIdOrderByNameAsc(courseId).stream()
                .sorted(MODULE_ORDER)
                .toList();
        Map<UUID, Boolean> unlockedMap = buildModuleUnlockedMap(modules, userId);

        List<ModuleDTO.ModuleListItem> items = modules.stream()
                .map(module -> new ModuleDTO.ModuleListItem(
                        module.getModuleId(),
                        module.getCourse() == null ? null : module.getCourse().getCourseId(),
                        module.getName(),
                        module.getDescription(),
                        lessonCounts.getOrDefault(module.getModuleId(), 0L),
                        toExamId(module.getExam()),
                        userId == null ? null : unlockedMap.get(module.getModuleId())
                ))
                .toList();

        return new ModuleDTO.ModuleListResponse(items);
    }

    public ModuleDTO.ModuleDetailResponse getModule(UUID moduleId) {
        ModuleEntity module = moduleRepository.findWithCourseAndExamByModuleId(moduleId)
                .orElseThrow(notFound("module_not_found"));

        ExemEntity exam = module.getExam();
        ModuleDTO.ModuleExamSummary summary = exam == null
                ? null
                : new ModuleDTO.ModuleExamSummary(
                exam.getExemId(),
                exam.getName(),
                safeList(exam.getQuests()).size(),
                safeList(exam.getTasks()).size()
        );

        return new ModuleDTO.ModuleDetailResponse(
                module.getModuleId(),
                module.getCourse() == null ? null : module.getCourse().getCourseId(),
                module.getName(),
                module.getDescription(),
                summary
        );
    }

    public ModuleDTO.ModuleLessonsResponse getModuleLessons(UUID moduleId) {
        return getModuleLessons(moduleId, null);
    }

    public ModuleDTO.ModuleLessonsResponse getModuleLessons(UUID moduleId, UUID userId) {
        ModuleEntity module = moduleRepository.findWithLessonsByModuleId(moduleId)
                .orElseThrow(notFound("module_not_found"));

        Boolean moduleUnlocked = userId == null ? null : learningAccessService.isModuleUnlocked(userId, moduleId);
        List<LessonEntity> orderedLessons = safeList(module.getLessons()).stream()
                .sorted(LESSON_ORDER)
                .toList();
        List<ModuleDTO.LessonCard> items = new java.util.ArrayList<>(orderedLessons.size());
        boolean previousCompleted = true;
        for (LessonEntity lesson : orderedLessons) {
            Boolean unlocked = null;
            if (userId != null) {
                unlocked = Boolean.TRUE.equals(moduleUnlocked) && previousCompleted;
                previousCompleted = progressQueryService.getLessonProgress(userId, lesson.getLessonId()).completed();
            }
            items.add(new ModuleDTO.LessonCard(
                    lesson.getLessonId(),
                    lesson.getName(),
                    lesson.getDescription(),
                    lesson.getXp(),
                    toQuizId(lesson.getQuiz()),
                    toTaskId(lesson.getTask()),
                    unlocked
            ));
        }

        return new ModuleDTO.ModuleLessonsResponse(items);
    }

    public ModuleDTO.ModuleExamResponse getModuleExam(UUID moduleId) {
        ExemEntity exam = examRepository.findWithRelationsByModule_ModuleId(moduleId)
                .orElseThrow(notFound("exam_not_found"));

        return new ModuleDTO.ModuleExamResponse(
                exam.getExemId(),
                exam.getModule() == null ? null : exam.getModule().getModuleId(),
                exam.getName(),
                exam.getDescription(),
                safeList(exam.getQuests()).size(),
                safeList(exam.getTasks()).size()
        );
    }

    private void attachLessonsToModule(ModuleEntity module, List<UUID> lessonIds) {
        UUID courseId = module.getCourse() == null ? null : module.getCourse().getCourseId();
        Set<UUID> existing = new HashSet<>();
        for (LessonEntity l : safeList(module.getLessons())) {
            if (l != null && l.getLessonId() != null) {
                existing.add(l.getLessonId());
            }
        }

        for (UUID lessonId : safeList(lessonIds)) {
            if (lessonId == null) {
                continue;
            }

            if (existing.contains(lessonId)) {
                continue;
            }

            UUID lessonCourseId = moduleRepository.findCourseIdByLessonId(lessonId);

            if (courseId != null && lessonCourseId != null && !courseId.equals(lessonCourseId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_course_mismatch");
            }
            if (courseId == null && lessonCourseId != null) {
                module.setCourse(courseRepository.findById(lessonCourseId).orElse(null));
                courseId = module.getCourse() == null ? null : module.getCourse().getCourseId();
            }

            LessonEntity lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(notFound("lesson_not_found"));
            module.getLessons().add(lesson);
            existing.add(lessonId);
        }
    }

    private ModuleDTO.ModuleDetailResponse toDetailResponse(ModuleEntity module) {
        ExemEntity exam = module.getExam();
        ModuleDTO.ModuleExamSummary summary = exam == null
                ? null
                : new ModuleDTO.ModuleExamSummary(
                exam.getExemId(),
                exam.getName(),
                safeList(exam.getQuests()).size(),
                safeList(exam.getTasks()).size()
        );

        return new ModuleDTO.ModuleDetailResponse(
                module.getModuleId(),
                module.getCourse() == null ? null : module.getCourse().getCourseId(),
                module.getName(),
                module.getDescription(),
                summary
        );
    }

    private UUID toExamId(ExemEntity exam) {
        return exam == null ? null : exam.getExemId();
    }

    private UUID toQuizId(QuizEntity quiz) {
        return quiz == null ? null : quiz.getQuizId();
    }

    private UUID toTaskId(TasksEntity task) {
        return task == null ? null : task.getTasksId();
    }

    private Supplier<ApiException> notFound(String errorCode) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, errorCode);
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private Map<UUID, Boolean> buildModuleUnlockedMap(List<ModuleEntity> modules, UUID userId) {
        if (userId == null) {
            return Map.of();
        }

        Map<UUID, Boolean> unlocked = new java.util.HashMap<>();
        boolean previousCompleted = true;
        for (ModuleEntity module : modules) {
            if (module == null || module.getModuleId() == null) {
                continue;
            }
            unlocked.put(module.getModuleId(), previousCompleted);
            boolean moduleCompleted = progressQueryService.getModuleProgress(userId, module.getModuleId()).completed();
            previousCompleted = moduleCompleted;
        }
        return unlocked;
    }
}
