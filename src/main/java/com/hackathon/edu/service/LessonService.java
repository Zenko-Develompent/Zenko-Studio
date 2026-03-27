package com.hackathon.edu.service;

import com.hackathon.edu.dto.lesson.LessonDTO;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.LessonRepository;
import com.hackathon.edu.repository.ModuleRepository;
import com.hackathon.edu.repository.QuizRepository;
import com.hackathon.edu.repository.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {
    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final QuizRepository quizRepository;
    private final TasksRepository tasksRepository;
    private final LessonContentService lessonContentService;
    private final LearningAccessService learningAccessService;

    @Transactional
    public LessonDTO.LessonDetailResponse createLesson(
            UUID moduleId,
            String nameRaw,
            String description,
            Integer xpRaw,
            MultipartFile file
    ) {
        if (nameRaw == null || nameRaw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "name_invalid");
        }
        String name = nameRaw.trim();
        if (name.length() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "name_invalid");
        }

        ModuleEntity module = moduleRepository.findById(moduleId)
                .orElseThrow(notFound("module_not_found"));

        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(UUID.randomUUID());
        lesson.setName(name);
        lesson.setDescription(description);
        lesson.setXp(xpRaw == null ? 0 : xpRaw);

        String storedPath = lessonContentService.storeLessonBody(lesson.getLessonId(), file);
        lesson.setBody(storedPath);

        module.getLessons().add(lesson);
        moduleRepository.saveAndFlush(module);

        String content = lessonContentService.readRawMarkdown(lesson.getBody());
        return new LessonDTO.LessonDetailResponse(
                lesson.getLessonId(),
                lesson.getName(),
                lesson.getDescription(),
                content,
                lesson.getXp(),
                null,
                null,
                null
        );
    }

    @Transactional
    public LessonDTO.LessonDetailResponse updateLesson(UUID lessonId, LessonDTO.LessonUpdateRequest request) {
        LessonEntity lesson = lessonRepository.findWithRelationsByLessonId(lessonId)
                .orElseThrow(notFound("lesson_not_found"));

        lesson.setName(request.name());
        lesson.setDescription(request.description());
        if (request.xp() != null) {
            lesson.setXp(request.xp());
        }

        String content = lessonContentService.readRawMarkdown(lesson.getBody());
        return new LessonDTO.LessonDetailResponse(
                lesson.getLessonId(),
                lesson.getName(),
                lesson.getDescription(),
                content,
                lesson.getXp(),
                toQuizId(lesson.getQuiz()),
                toTaskId(lesson.getTask()),
                null
        );
    }

    @Transactional
    public void replaceLessonBody(UUID lessonId, MultipartFile file) {
        LessonEntity lesson = lessonRepository.findWithRelationsByLessonId(lessonId)
                .orElseThrow(notFound("lesson_not_found"));

        String previous = lesson.getBody();
        String storedPath = lessonContentService.storeLessonBody(lesson.getLessonId(), file);
        lesson.setBody(storedPath);
        lessonContentService.deleteLessonBodyIfExists(previous);
    }

    @Transactional
    public void setLessonBodyLink(UUID lessonId, String body) {
        LessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(notFound("lesson_not_found"));

        lessonContentService.resolveForDownload(body);
        lesson.setBody(body);
    }

    public LessonContentService.ResolvedLessonFile getLessonBodyFile(UUID userId, UUID lessonId) {
        learningAccessService.assertLessonUnlocked(userId, lessonId);
        LessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(notFound("lesson_not_found"));
        return lessonContentService.resolveForDownload(lesson.getBody());
    }

    @Transactional
    public void deleteLesson(UUID lessonId) {
        LessonEntity lesson = lessonRepository.findWithRelationsByLessonId(lessonId)
                .orElseThrow(notFound("lesson_not_found"));

        QuizEntity quiz = lesson.getQuiz();
        if (quiz != null && quiz.getQuizId() != null) {
            quizRepository.delete(quiz);
        }

        TasksEntity task = lesson.getTask();
        if (task != null && task.getTasksId() != null) {
            task.setLesson(null);
        }

        String body = lesson.getBody();
        lessonRepository.delete(lesson);
        lessonContentService.deleteLessonBodyIfExists(body);
    }

    public LessonDTO.LessonDetailResponse getLesson(UUID userId, UUID lessonId) {
        LessonEntity lesson = lessonRepository.findWithRelationsByLessonId(lessonId)
                .orElseThrow(notFound("lesson_not_found"));
        Boolean unlocked = learningAccessService.isLessonUnlocked(userId, lessonId);
        String content = Boolean.TRUE.equals(unlocked)
                ? lessonContentService.readRawMarkdown(lesson.getBody())
                : null;

        return new LessonDTO.LessonDetailResponse(
                lesson.getLessonId(),
                lesson.getName(),
                lesson.getDescription(),
                content,
                lesson.getXp(),
                toQuizId(lesson.getQuiz()),
                toTaskId(lesson.getTask()),
                unlocked
        );
    }

    public LessonDTO.LessonQuizResponse getLessonQuiz(UUID userId, UUID lessonId) {
        learningAccessService.assertLessonUnlocked(userId, lessonId);
        QuizEntity quiz = quizRepository.findWithQuestsByLesson_LessonId(lessonId)
                .orElseThrow(notFound("quiz_not_found"));

        return new LessonDTO.LessonQuizResponse(
                quiz.getQuizId(),
                quiz.getLesson() == null ? null : quiz.getLesson().getLessonId(),
                quiz.getName(),
                quiz.getDescription(),
                quiz.getQuests() == null ? 0 : quiz.getQuests().size()
        );
    }

    public LessonDTO.LessonTaskResponse getLessonTask(UUID userId, UUID lessonId) {
        learningAccessService.assertLessonUnlocked(userId, lessonId);
        TasksEntity task = tasksRepository.findByLesson_LessonId(lessonId)
                .orElseThrow(notFound("task_not_found"));

        return new LessonDTO.LessonTaskResponse(
                task.getTasksId(),
                task.getLesson() == null ? null : task.getLesson().getLessonId(),
                task.getExam() == null ? null : task.getExam().getExemId(),
                task.getName(),
                task.getDescription(),
                task.getRunnerLanguage(),
                safeInt(task.getXpReward()),
                safeInt(task.getCoinReward())
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

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
