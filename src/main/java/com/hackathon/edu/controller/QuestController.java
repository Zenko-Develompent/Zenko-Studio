package com.hackathon.edu.controller;

import com.hackathon.edu.dto.quiz.QuizDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.QuizService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {
    private final QuizService quizService;
    private final AuthService authService;

    @GetMapping("/{questId}/answers")
    public QuizDTO.AnswersResponse questAnswers(
            @PathVariable("questId") UUID questId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        return quizService.getQuestAnswers(userId, questId);
    }
    //zenkoz
    @PostMapping("/{questId}/check")
    public QuizDTO.CheckAnswerResponse checkAnswer(
            @PathVariable("questId") UUID questId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody QuizDTO.CheckAnswerRequest request
    ) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        return quizService.checkQuestAnswer(questId, request.answerId(), userId);
    }

    private UUID resolveOptionalUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        return authService.requireUserIdFromAccessHeader(authorizationHeader);
    }
}

