package com.hackathon.edu.controller;

import com.hackathon.edu.dto.quiz.QuizDTO;
import com.hackathon.edu.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizService;

    @GetMapping("/{quizId}/questions")
    public QuizDTO.QuestionsResponse quizQuestions(@PathVariable("quizId") UUID quizId) {
        return quizService.getQuizQuestions(quizId);
    }
}

