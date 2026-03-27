package com.hackathon.edu.controller;

import com.hackathon.edu.dto.exam.ExamDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;
    private final AuthService authService;

    @GetMapping("/{examId}")
    public ExamDTO.ExamDetailResponse exam(
            @PathVariable("examId") UUID examId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        return examService.getExam(userId, examId);
    }
    //zenkoa
    @GetMapping("/{examId}/questions")
    public ExamDTO.QuestionsResponse examQuestions(
            @PathVariable("examId") UUID examId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return examService.getExamQuestions(userId, examId);
    }

    @GetMapping("/{examId}/tasks")
    public ExamDTO.TasksResponse examTasks(
            @PathVariable("examId") UUID examId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return examService.getExamTasks(userId, examId);
    }

    @PostMapping("/{examId}/complete")
    public ExamDTO.CompleteResponse completeExam(
            @PathVariable("examId") UUID examId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return examService.completeExam(userId, examId);
    }

    @PutMapping("/{examId}/rewards")
    public ExamDTO.ExamDetailResponse updateExamRewards(
            @PathVariable("examId") UUID examId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody ExamDTO.UpdateRewardsRequest request
    ) {
        authService.requireAdminUserIdFromAccessHeader(authorizationHeader);
        return examService.updateExamRewards(examId, request);
    }

    private UUID resolveOptionalUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        return authService.requireUserIdFromAccessHeader(authorizationHeader);
    }
}
