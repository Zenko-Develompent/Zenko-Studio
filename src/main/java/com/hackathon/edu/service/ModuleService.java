package com.hackathon.edu.service;

import com.hackathon.edu.dto.module.ModuleDTO;
import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.ExemRepository;
import com.hackathon.edu.repository.ModuleRepository;
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
public class ModuleService {
    private static final Comparator<LessonEntity> LESSON_ORDER = Comparator
            .comparing(LessonEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(LessonEntity::getLessonId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ModuleRepository moduleRepository;
    private final ExemRepository examRepository;

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
        ModuleEntity module = moduleRepository.findWithLessonsByModuleId(moduleId)
                .orElseThrow(notFound("module_not_found"));

        List<ModuleDTO.LessonCard> items = safeList(module.getLessons()).stream()
                .sorted(LESSON_ORDER)
                .map(lesson -> new ModuleDTO.LessonCard(
                        lesson.getLessonId(),
                        lesson.getName(),
                        lesson.getDescription(),
                        lesson.getBody(),
                        lesson.getXp(),
                        toQuizId(lesson.getQuiz()),
                        toTaskId(lesson.getTask())
                ))
                .toList();

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
