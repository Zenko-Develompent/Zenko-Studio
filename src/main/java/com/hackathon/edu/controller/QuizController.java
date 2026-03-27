package com.hackathon.edu.controller;

import com.hackathon.edu.dto.quiz.QuizFlowDTO;
import com.hackathon.edu.dto.quiz.QuizDTO;
import com.hackathon.edu.service.AuthService;
import jakarta.validation.Valid;
import com.hackathon.edu.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizService;
    private final AuthService authService;

    @PostMapping("/lessons/{lessonId}")
    public QuizDTO.QuizDetailResponse createLessonQuiz(
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody QuizDTO.QuizCreateRequest request
    ) {
        return quizService.createLessonQuiz(lessonId, request);
    }
    //ozenko
    @GetMapping("/lessons/{lessonId}")
    public QuizDTO.QuizDetailResponse quizByLesson(@PathVariable("lessonId") UUID lessonId) {
        return quizService.getQuizByLesson(lessonId);
    }

    @GetMapping("/{quizId}")
    public QuizDTO.QuizDetailResponse quiz(@PathVariable("quizId") UUID quizId) {
        return quizService.getQuiz(quizId);
    }

    @GetMapping("/{quizId}/questions")
    public QuizDTO.QuestionsResponse quizQuestions(@PathVariable("quizId") UUID quizId) {
        return quizService.getQuizQuestions(quizId);
    }

    @PostMapping("/{quizId}/start")
    public QuizFlowDTO.StartResponse startQuiz(
            @PathVariable("quizId") UUID quizId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return quizService.startQuiz(userId, quizId);
    }

    @PostMapping("/{quizId}/answer")
    public QuizFlowDTO.SubmitAnswerResponse submitAnswer(
            @PathVariable("quizId") UUID quizId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody QuizFlowDTO.SubmitAnswerRequest request
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return quizService.submitAnswer(userId, quizId, request);
    }
}
