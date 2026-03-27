package com.hackathon.edu.controller;

import com.hackathon.edu.dto.quiz.QuizDTO;
import com.hackathon.edu.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {
    private final QuizService quizService;

    @GetMapping("/{questId}/answers")
    public QuizDTO.AnswersResponse questAnswers(@PathVariable("questId") UUID questId) {
        return quizService.getQuestAnswers(questId);
    }

    @PostMapping("/{questId}/check")
    public QuizDTO.CheckAnswerResponse checkAnswer(
            @PathVariable("questId") UUID questId,
            @Valid @RequestBody QuizDTO.CheckAnswerRequest request
    ) {
        return quizService.checkQuestAnswer(questId, request.answerId());
    }
}

