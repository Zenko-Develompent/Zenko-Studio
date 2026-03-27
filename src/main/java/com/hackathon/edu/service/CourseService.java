package com.hackathon.edu.service;

import com.hackathon.edu.dto.course.CourseDTO;
import com.hackathon.edu.dto.module.ModuleDTO;
import com.hackathon.edu.entity.CourseEntity;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.CourseRepository;
import com.hackathon.edu.repository.LessonRepository;
import com.hackathon.edu.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {
    private static final Comparator<ModuleEntity> MODULE_ORDER = Comparator
            .comparing(ModuleEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(ModuleEntity::getModuleId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<LessonEntity> LESSON_ORDER = Comparator
            .comparing(LessonEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(LessonEntity::getLessonId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final ProgressQueryService progressQueryService;

    public CourseDTO.CourseListResponse listCourses(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        var slice = courseRepository.findAllByOrderByNameAsc(PageRequest.of(safePage, safeSize));
        List<CourseDTO.CourseListItem> items = slice.stream()
                .map(this::toCourseListItem)
                .toList();

        return new CourseDTO.CourseListResponse(
                items,
                slice.getNumber(),
                slice.getSize(),
                slice.getTotalElements()
        );
    }

    public CourseDTO.CourseDetailResponse getCourse(UUID courseId) {
        return getCourse(courseId, null);
    }

    public CourseDTO.CourseDetailResponse getCourse(UUID courseId, UUID userId) {
        CourseEntity course = courseRepository.findWithModulesByCourseId(courseId)
                .orElseThrow(notFound("course_not_found"));

        List<ModuleEntity> orderedModules = safeList(course.getModules()).stream()
                .sorted(MODULE_ORDER)
                .toList();
        Map<UUID, Boolean> unlockedMap = buildModuleUnlockedMap(orderedModules, userId);
        List<CourseDTO.CourseModuleItem> modules = orderedModules.stream()
                .map(module -> toCourseModuleItem(module, userId == null ? null : unlockedMap.get(module.getModuleId())))
                .toList();

        return new CourseDTO.CourseDetailResponse(
                course.getCourseId(),
                course.getName(),
                course.getDescription(),
                course.getCategory(),
                modules
        );
    }

    public CourseDTO.CourseModulesResponse getCourseModules(UUID courseId) {
        return getCourseModules(courseId, null);
    }

    public CourseDTO.CourseModulesResponse getCourseModules(UUID courseId, UUID userId) {
        CourseEntity course = courseRepository.findWithModulesByCourseId(courseId)
                .orElseThrow(notFound("course_not_found"));

        List<ModuleEntity> orderedModules = safeList(course.getModules()).stream()
                .sorted(MODULE_ORDER)
                .toList();
        Map<UUID, Boolean> unlockedMap = buildModuleUnlockedMap(orderedModules, userId);
        List<CourseDTO.CourseModuleItem> items = orderedModules.stream()
                .map(module -> toCourseModuleItem(module, userId == null ? null : unlockedMap.get(module.getModuleId())))
                .toList();

        return new CourseDTO.CourseModulesResponse(items);
    }

    public CourseDTO.CourseTreeResponse getCourseTree(UUID courseId) {
        return getCourseTree(courseId, null);
    }

    public CourseDTO.CourseTreeResponse getCourseTree(UUID courseId, UUID userId) {
        CourseEntity course = courseRepository.findWithTreeByCourseId(courseId)
                .orElseThrow(notFound("course_not_found"));

        List<ModuleEntity> orderedModules = safeList(course.getModules()).stream()
                .sorted(MODULE_ORDER)
                .toList();
        Map<UUID, Boolean> moduleUnlocked = buildModuleUnlockedMap(orderedModules, userId);

        List<CourseDTO.CourseTreeModuleItem> modules = new java.util.ArrayList<>(orderedModules.size());
        for (ModuleEntity module : orderedModules) {
            UUID moduleId = module.getModuleId();
            Boolean unlocked = userId == null ? null : moduleUnlocked.get(moduleId);

            List<LessonEntity> orderedLessons = safeList(module.getLessons()).stream()
                    .sorted(LESSON_ORDER)
                    .toList();
            List<CourseDTO.CourseTreeLessonItem> lessonItems = new java.util.ArrayList<>(orderedLessons.size());
            boolean previousCompleted = true;
            for (LessonEntity lesson : orderedLessons) {
                Boolean lessonUnlocked = null;
                if (userId != null) {
                    lessonUnlocked = Boolean.TRUE.equals(unlocked) && previousCompleted;
                    previousCompleted = progressQueryService.getLessonProgress(userId, lesson.getLessonId()).completed();
                }
                lessonItems.add(new CourseDTO.CourseTreeLessonItem(
                        lesson.getLessonId(),
                        lesson.getName(),
                        toQuizId(lesson.getQuiz()),
                        toTaskId(lesson.getTask()),
                        lessonUnlocked
                ));
            }

            modules.add(new CourseDTO.CourseTreeModuleItem(
                    moduleId,
                    module.getName(),
                    toExamId(module.getExam()),
                    unlocked,
                    lessonItems
            ));
        }

        return new CourseDTO.CourseTreeResponse(course.getCourseId(), course.getName(), modules);
    }

    private CourseDTO.CourseListItem toCourseListItem(CourseEntity course) {
        long modules = moduleRepository.countByCourse_CourseId(course.getCourseId());
        long lessons = lessonRepository.countByCourseId(course.getCourseId());

        return new CourseDTO.CourseListItem(
                course.getCourseId(),
                course.getName(),
                course.getDescription(),
                course.getCategory(),
                modules,
                lessons
            );
    }

    private CourseDTO.CourseModuleItem toCourseModuleItem(ModuleEntity module, Boolean unlocked) {
        long lessonCount = safeList(module.getLessons()).size();
        return new CourseDTO.CourseModuleItem(
                module.getModuleId(),
                module.getName(),
                module.getDescription(),
                lessonCount,
                toExamId(module.getExam()),
                unlocked
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
            previousCompleted = progressQueryService.getModuleProgress(userId, module.getModuleId()).completed();
        }
        return unlocked;
    }


//    ==========================

    @Transactional
    public CourseDTO.CourseDetailResponse createCourse(CourseDTO.CreateCourseRequest request) {
        CourseEntity course = new CourseEntity();
        course.setName(request.name());
        course.setDescription(request.description());
        course.setCategory(request.category());

        course = courseRepository.save(course);

        createModules(course, request.modules());

        return getCourse(course.getCourseId());
    }

    private void createModules(CourseEntity course, List<CourseDTO.ModuleCreateRequest> modules) {

        safeList(modules).forEach(request -> {
            ModuleEntity module = new ModuleEntity();
            module.setName(request.name());
            module.setDescription(request.description());

            course.addModule(module);
        });
    }

//    private void attachModulesToCourse(CourseEntity course, List<UUID> moduleIds) {
//
//        for (UUID moduleId : safeList(moduleIds)) {
//            if (moduleId == null) {
//                continue;
//            }
//
//            ModuleEntity Module = moduleRepository.findWithRelationsByModuleId(moduleId)
//                    .orElseThrow(notFound("lesson_not_found"));
//
//            UUID lessonCourseId = module.getCourse() == null || module.getCourse().getCourse() == null
//                    ? null
//                    : lesson.getModule().getCourse().getCourseId();
//            if (courseId != null && lessonCourseId != null && !courseId.equals(lessonCourseId)) {
//                throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_course_mismatch");
//            }
//
//            lesson.setModule(module);
//        }
//    }
}
