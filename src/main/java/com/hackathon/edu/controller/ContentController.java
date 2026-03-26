package com.hackathon.edu.controller;

import com.hackathon.edu.dto.content.ContentDtos;
import com.hackathon.edu.service.ContentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContentController {
    private final ContentQueryService contentQueryService;

    @GetMapping("/courses")
    public ContentDtos.CourseListResponse courses(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return contentQueryService.listCourses(page, size);
    }

    @GetMapping("/courses/{courseId}")
    public ContentDtos.CourseDetailResponse course(@PathVariable("courseId") UUID courseId) {
        return contentQueryService.getCourse(courseId);
    }

    @GetMapping("/courses/{courseId}/modules")
    public ContentDtos.CourseModulesResponse courseModules(@PathVariable("courseId") UUID courseId) {
        return contentQueryService.getCourseModules(courseId);
    }

    @GetMapping("/courses/{courseId}/tree")
    public ContentDtos.CourseTreeResponse courseTree(@PathVariable("courseId") UUID courseId) {
        return contentQueryService.getCourseTree(courseId);
    }

    @GetMapping("/modules/{moduleId}")
    public ContentDtos.ModuleDetailResponse module(@PathVariable("moduleId") UUID moduleId) {
        return contentQueryService.getModule(moduleId);
    }

    @GetMapping("/modules/{moduleId}/lessons")
    public ContentDtos.ModuleLessonsResponse moduleLessons(@PathVariable("moduleId") UUID moduleId) {
        return contentQueryService.getModuleLessons(moduleId);
    }

    @GetMapping("/modules/{moduleId}/exam")
    public ContentDtos.ModuleExamResponse moduleExam(@PathVariable("moduleId") UUID moduleId) {
        return contentQueryService.getModuleExam(moduleId);
    }

    @GetMapping("/lessons/{lessonId}")
    public ContentDtos.LessonDetailResponse lesson(@PathVariable("lessonId") UUID lessonId) {
        return contentQueryService.getLesson(lessonId);
    }

    @GetMapping("/lessons/{lessonId}/quiz")
    public ContentDtos.LessonQuizResponse lessonQuiz(@PathVariable("lessonId") UUID lessonId) {
        return contentQueryService.getLessonQuiz(lessonId);
    }

    @GetMapping("/lessons/{lessonId}/task")
    public ContentDtos.LessonTaskResponse lessonTask(@PathVariable("lessonId") UUID lessonId) {
        return contentQueryService.getLessonTask(lessonId);
    }

    @GetMapping("/quizzes/{quizId}/questions")
    public ContentDtos.QuestionsResponse quizQuestions(@PathVariable("quizId") UUID quizId) {
        return contentQueryService.getQuizQuestions(quizId);
    }

    @GetMapping("/exams/{examId}")
    public ContentDtos.ExamDetailResponse exam(@PathVariable("examId") UUID examId) {
        return contentQueryService.getExam(examId);
    }

    @GetMapping("/exams/{examId}/questions")
    public ContentDtos.QuestionsResponse examQuestions(@PathVariable("examId") UUID examId) {
        return contentQueryService.getExamQuestions(examId);
    }

    @GetMapping("/exams/{examId}/tasks")
    public ContentDtos.TasksResponse examTasks(@PathVariable("examId") UUID examId) {
        return contentQueryService.getExamTasks(examId);
    }
}

