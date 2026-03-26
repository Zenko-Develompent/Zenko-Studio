package com.hackathon.edu.controller;

import com.hackathon.edu.dto.exam.ExamDTO;
import com.hackathon.edu.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;

    @GetMapping("/{examId}")
    public ExamDTO.ExamDetailResponse exam(@PathVariable("examId") UUID examId) {
        return examService.getExam(examId);
    }

    @GetMapping("/{examId}/questions")
    public ExamDTO.QuestionsResponse examQuestions(@PathVariable("examId") UUID examId) {
        return examService.getExamQuestions(examId);
    }

    @GetMapping("/{examId}/tasks")
    public ExamDTO.TasksResponse examTasks(@PathVariable("examId") UUID examId) {
        return examService.getExamTasks(examId);
    }
}

