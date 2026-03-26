package com.hackathon.edu.service;

import com.hackathon.edu.dto.content.ContentDtos;
import com.hackathon.edu.entity.CourseEntity;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.QuestEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.CourseRepository;
import com.hackathon.edu.repository.ExemRepository;
import com.hackathon.edu.repository.LessonRepository;
import com.hackathon.edu.repository.ModuleRepository;
import com.hackathon.edu.repository.QuizRepository;
import com.hackathon.edu.repository.TasksRepository;
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
public class ContentQueryService {
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
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final ExemRepository exemRepository;
    private final TasksRepository tasksRepository;

    public ContentDtos.CourseListResponse listCourses(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        var slice = courseRepository.findAllByOrderByNameAsc(PageRequest.of(safePage, safeSize));
        List<ContentDtos.CourseListItem> items = slice.stream()
                .map(this::toCourseListItem)
                .toList();

        return new ContentDtos.CourseListResponse(
                items,
                slice.getNumber(),
                slice.getSize(),
                slice.getTotalElements()
        );
    }

    public ContentDtos.CourseDetailResponse getCourse(UUID courseId) {
        CourseEntity course = courseRepository.findWithModulesByCourseId(courseId)
                .orElseThrow(notFound("course_not_found"));

        List<ContentDtos.CourseModuleItem> modules = safeList(course.getModules()).stream()
                .sorted(MODULE_ORDER)
                .map(this::toCourseModuleItem)
                .toList();

        return new ContentDtos.CourseDetailResponse(
                course.getCourseId(),
                course.getName(),
                course.getDescription(),
                course.getCategory(),
                modules
        );
    }

    public ContentDtos.CourseModulesResponse getCourseModules(UUID courseId) {
        CourseEntity course = courseRepository.findWithModulesByCourseId(courseId)
                .orElseThrow(notFound("course_not_found"));

        List<ContentDtos.CourseModuleItem> items = safeList(course.getModules()).stream()
                .sorted(MODULE_ORDER)
                .map(this::toCourseModuleItem)
                .toList();

        return new ContentDtos.CourseModulesResponse(items);
    }

    public ContentDtos.CourseTreeResponse getCourseTree(UUID courseId) {
        CourseEntity course = courseRepository.findWithTreeByCourseId(courseId)
                .orElseThrow(notFound("course_not_found"));

        List<ContentDtos.CourseTreeModuleItem> modules = safeList(course.getModules()).stream()
                .sorted(MODULE_ORDER)
                .map(module -> new ContentDtos.CourseTreeModuleItem(
                        module.getModuleId(),
                        module.getName(),
                        toExamId(module.getExam()),
                        safeList(module.getLessons()).stream()
                                .sorted(LESSON_ORDER)
                                .map(lesson -> new ContentDtos.CourseTreeLessonItem(
                                        lesson.getLessonId(),
                                        lesson.getName(),
                                        lesson.getBody(),
                                        toQuizId(lesson.getQuiz()),
                                        toTaskId(lesson.getTask())
                                ))
                                .toList()
                ))
                .toList();

        return new ContentDtos.CourseTreeResponse(course.getCourseId(), course.getName(), modules);
    }

    public ContentDtos.ModuleDetailResponse getModule(UUID moduleId) {
        ModuleEntity module = moduleRepository.findWithCourseAndExamByModuleId(moduleId)
                .orElseThrow(notFound("module_not_found"));

        ExemEntity exam = module.getExam();
        ContentDtos.ModuleExamSummary summary = exam == null
                ? null
                : new ContentDtos.ModuleExamSummary(
                exam.getExemId(),
                exam.getName(),
                safeList(exam.getQuests()).size(),
                safeList(exam.getTasks()).size()
        );

        return new ContentDtos.ModuleDetailResponse(
                module.getModuleId(),
                module.getCourse() == null ? null : module.getCourse().getCourseId(),
                module.getName(),
                module.getDescription(),
                summary
        );
    }

    public ContentDtos.ModuleLessonsResponse getModuleLessons(UUID moduleId) {
        ModuleEntity module = moduleRepository.findWithLessonsByModuleId(moduleId)
                .orElseThrow(notFound("module_not_found"));

        List<ContentDtos.LessonCard> items = safeList(module.getLessons()).stream()
                .sorted(LESSON_ORDER)
                .map(this::toLessonCard)
                .toList();

        return new ContentDtos.ModuleLessonsResponse(items);
    }

    public ContentDtos.ModuleExamResponse getModuleExam(UUID moduleId) {
        ExemEntity exam = exemRepository.findWithRelationsByModule_ModuleId(moduleId)
                .orElseThrow(notFound("exam_not_found"));

        return toModuleExamResponse(exam);
    }

    public ContentDtos.LessonDetailResponse getLesson(UUID lessonId) {
        LessonEntity lesson = lessonRepository.findWithRelationsByLessonId(lessonId)
                .orElseThrow(notFound("lesson_not_found"));

        return new ContentDtos.LessonDetailResponse(
                lesson.getLessonId(),
                lesson.getModule() == null ? null : lesson.getModule().getModuleId(),
                lesson.getName(),
                lesson.getDescription(),
                lesson.getBody(),
                lesson.getXp(),
                toQuizId(lesson.getQuiz()),
                toTaskId(lesson.getTask())
        );
    }

    public ContentDtos.LessonQuizResponse getLessonQuiz(UUID lessonId) {
        QuizEntity quiz = quizRepository.findWithQuestsByLesson_LessonId(lessonId)
                .orElseThrow(notFound("quiz_not_found"));

        return new ContentDtos.LessonQuizResponse(
                quiz.getQuizId(),
                quiz.getLesson() == null ? null : quiz.getLesson().getLessonId(),
                quiz.getName(),
                quiz.getDescription(),
                safeList(quiz.getQuests()).size()
        );
    }

    public ContentDtos.LessonTaskResponse getLessonTask(UUID lessonId) {
        TasksEntity task = tasksRepository.findByLesson_LessonId(lessonId)
                .orElseThrow(notFound("task_not_found"));

        return toLessonTaskResponse(task);
    }

    public ContentDtos.QuestionsResponse getQuizQuestions(UUID quizId) {
        QuizEntity quiz = quizRepository.findWithQuestsByQuizId(quizId)
                .orElseThrow(notFound("quiz_not_found"));

        List<ContentDtos.QuestionItem> items = safeList(quiz.getQuests()).stream()
                .sorted(QUESTION_ORDER)
                .map(this::toQuestionItem)
                .toList();

        return new ContentDtos.QuestionsResponse(items);
    }

    public ContentDtos.ExamDetailResponse getExam(UUID examId) {
        ExemEntity exam = exemRepository.findWithRelationsByExemId(examId)
                .orElseThrow(notFound("exam_not_found"));

        return toExamDetailResponse(exam);
    }

    public ContentDtos.QuestionsResponse getExamQuestions(UUID examId) {
        ExemEntity exam = exemRepository.findWithRelationsByExemId(examId)
                .orElseThrow(notFound("exam_not_found"));

        List<ContentDtos.QuestionItem> items = safeList(exam.getQuests()).stream()
                .sorted(QUESTION_ORDER)
                .map(this::toQuestionItem)
                .toList();

        return new ContentDtos.QuestionsResponse(items);
    }

    public ContentDtos.TasksResponse getExamTasks(UUID examId) {
        ExemEntity exam = exemRepository.findWithRelationsByExemId(examId)
                .orElseThrow(notFound("exam_not_found"));

        List<ContentDtos.TaskItem> items = safeList(exam.getTasks()).stream()
                .sorted(TASK_ORDER)
                .map(this::toTaskItem)
                .toList();

        return new ContentDtos.TasksResponse(items);
    }

    private ContentDtos.CourseListItem toCourseListItem(CourseEntity course) {
        long modules = moduleRepository.countByCourse_CourseId(course.getCourseId());
        long lessons = lessonRepository.countByCourseId(course.getCourseId());
        return new ContentDtos.CourseListItem(
                course.getCourseId(),
                course.getName(),
                course.getDescription(),
                course.getCategory(),
                modules,
                lessons
        );
    }

    private ContentDtos.CourseModuleItem toCourseModuleItem(ModuleEntity module) {
        long lessonCount = safeList(module.getLessons()).size();
        return new ContentDtos.CourseModuleItem(
                module.getModuleId(),
                module.getName(),
                module.getDescription(),
                lessonCount,
                toExamId(module.getExam())
        );
    }

    private ContentDtos.LessonCard toLessonCard(LessonEntity lesson) {
        return new ContentDtos.LessonCard(
                lesson.getLessonId(),
                lesson.getName(),
                lesson.getDescription(),
                lesson.getBody(),
                lesson.getXp(),
                toQuizId(lesson.getQuiz()),
                toTaskId(lesson.getTask())
        );
    }

    private ContentDtos.ModuleExamResponse toModuleExamResponse(ExemEntity exam) {
        return new ContentDtos.ModuleExamResponse(
                exam.getExemId(),
                exam.getModule() == null ? null : exam.getModule().getModuleId(),
                exam.getName(),
                exam.getDescription(),
                safeList(exam.getQuests()).size(),
                safeList(exam.getTasks()).size()
        );
    }

    private ContentDtos.ExamDetailResponse toExamDetailResponse(ExemEntity exam) {
        return new ContentDtos.ExamDetailResponse(
                exam.getExemId(),
                exam.getModule() == null ? null : exam.getModule().getModuleId(),
                exam.getName(),
                exam.getDescription(),
                safeList(exam.getQuests()).size(),
                safeList(exam.getTasks()).size()
        );
    }

    private ContentDtos.LessonTaskResponse toLessonTaskResponse(TasksEntity task) {
        return new ContentDtos.LessonTaskResponse(
                task.getTasksId(),
                task.getLesson() == null ? null : task.getLesson().getLessonId(),
                task.getExam() == null ? null : task.getExam().getExemId(),
                task.getName(),
                task.getDescription()
        );
    }

    private ContentDtos.QuestionItem toQuestionItem(QuestEntity question) {
        return new ContentDtos.QuestionItem(
                question.getQuestId(),
                question.getQuiz() == null ? null : question.getQuiz().getQuizId(),
                question.getExam() == null ? null : question.getExam().getExemId(),
                question.getName(),
                question.getDescription()
        );
    }

    private ContentDtos.TaskItem toTaskItem(TasksEntity task) {
        return new ContentDtos.TaskItem(
                task.getTasksId(),
                task.getExam() == null ? null : task.getExam().getExemId(),
                task.getLesson() == null ? null : task.getLesson().getLessonId(),
                task.getName(),
                task.getDescription()
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
