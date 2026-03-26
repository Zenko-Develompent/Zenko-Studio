package com.hackathon.edu.service;

import com.hackathon.edu.dto.course.CourseDTO;
import com.hackathon.edu.entity.CourseEntity;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.QuestEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
public class CourseService {
    private static final Comparator<ModuleEntity> MODULE_ORDER = Comparator
            .comparing(ModuleEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(ModuleEntity::getModuleId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<LessonEntity> LESSON_ORDER = Comparator
            .comparing(LessonEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(LessonEntity::getLessonId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<QuestEntity> QUESTION_ORDER = Comparator
            .comparing(QuestEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(QuestEntity::getQuestId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<TasksEntity> TASK_ORDER = Comparator
            .comparing(TasksEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TasksEntity::getTasksId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final CourseRepository courseRepository;

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
        CourseEntity course = courseRepository.findWithModulesByCourseId(courseId)
                .orElseThrow(notFound("course_not_found"));

        List<CourseDTO.CourseModuleItem> modules = safeList(course.getModules()).stream()
                .sorted(MODULE_ORDER)
                .map(this::toCourseModuleItem)
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
        CourseEntity course = courseRepository.findWithModulesByCourseId(courseId)
                .orElseThrow(notFound("course_not_found"));

        List<CourseDTO.CourseModuleItem> items = safeList(course.getModules()).stream()
                .sorted(MODULE_ORDER)
                .map(this::toCourseModuleItem)
                .toList();

        return new CourseDTO.CourseModulesResponse(items);
    }

    public CourseDTO.CourseTreeResponse getCourseTree(UUID courseId) {
        CourseEntity course = courseRepository.findWithTreeByCourseId(courseId)
                .orElseThrow(notFound("course_not_found"));

        List<CourseDTO.CourseTreeModuleItem> modules = safeList(course.getModules()).stream()
                .sorted(MODULE_ORDER)
                .map(module -> new CourseDTO.CourseTreeModuleItem(
                        module.getModuleId(),
                        module.getName(),
                        toExamId(module.getExam()),
                        safeList(module.getLessons()).stream()
                                .sorted(LESSON_ORDER)
                                .map(lesson -> new CourseDTO.CourseTreeLessonItem(
                                        lesson.getLessonId(),
                                        lesson.getName(),
                                        lesson.getBody(),
                                        toQuizId(lesson.getQuiz()),
                                        toTaskId(lesson.getTask())
                                ))
                                .toList()
                ))
                .toList();

        return new CourseDTO.CourseTreeResponse(course.getCourseId(), course.getName(), modules);
    }

    private CourseDTO.CourseListItem toCourseListItem(CourseEntity course) {

        return new CourseDTO.CourseListItem(
                course.getCourseId(),
                course.getName(),
                course.getDescription(),
                course.getCategory(),
                modules,
                lessons
        );
    }

    private CourseDTO.CourseModuleItem toCourseModuleItem(ModuleEntity module) {
        long lessonCount = safeList(module.getLessons()).size();
        return new CourseDTO.CourseModuleItem(
                module.getModuleId(),
                module.getName(),
                module.getDescription(),
                lessonCount,
                toExamId(module.getExam())
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
}
