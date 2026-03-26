package com.hackathon.edu.controller;

import com.hackathon.edu.dto.lesson.LessonDTO;
import com.hackathon.edu.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;

    @GetMapping("/{lessonId}")
    public LessonDTO.LessonDetailResponse lesson(@PathVariable("lessonId") UUID lessonId) {
        return lessonService.getLesson(lessonId);
    }

    @GetMapping("/{lessonId}/quiz")
    public LessonDTO.LessonQuizResponse lessonQuiz(@PathVariable("lessonId") UUID lessonId) {
        return lessonService.getLessonQuiz(lessonId);
    }

    @GetMapping("/{lessonId}/task")
    public LessonDTO.LessonTaskResponse lessonTask(@PathVariable("lessonId") UUID lessonId) {
        return lessonService.getLessonTask(lessonId);
    }
}

