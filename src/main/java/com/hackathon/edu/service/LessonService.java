package com.hackathon.edu.service;

import com.hackathon.edu.dto.lesson.LessonDTO;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.LessonRepository;
import com.hackathon.edu.repository.QuizRepository;
import com.hackathon.edu.repository.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final TasksRepository tasksRepository;
    private final LessonContentService lessonContentService;

    public LessonDTO.LessonDetailResponse getLesson(UUID lessonId) {
        LessonEntity lesson = lessonRepository.findWithRelationsByLessonId(lessonId)
                .orElseThrow(notFound("lesson_not_found"));
        String content = lessonContentService.readRawMarkdown(lesson.getBody());

        return new LessonDTO.LessonDetailResponse(
                lesson.getLessonId(),
                lesson.getModule() == null ? null : lesson.getModule().getModuleId(),
                lesson.getName(),
                lesson.getDescription(),
                content,
                lesson.getXp(),
                toQuizId(lesson.getQuiz()),
                toTaskId(lesson.getTask())
        );
    }

    public LessonDTO.LessonQuizResponse getLessonQuiz(UUID lessonId) {
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

    public LessonDTO.LessonTaskResponse getLessonTask(UUID lessonId) {
        TasksEntity task = tasksRepository.findByLesson_LessonId(lessonId)
                .orElseThrow(notFound("task_not_found"));

        return new LessonDTO.LessonTaskResponse(
                task.getTasksId(),
                task.getLesson() == null ? null : task.getLesson().getLessonId(),
                task.getExam() == null ? null : task.getExam().getExemId(),
                task.getName(),
                task.getDescription()
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
}
