package com.hackathon.edu.controller;

import com.hackathon.edu.dto.task.TaskDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final ProgressService progressService;
    private final AuthService authService;

    @PostMapping("/{taskId}/complete")
    public TaskDTO.CompleteResponse completeTask(
            @PathVariable("taskId") UUID taskId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        ProgressService.TaskCompletionResult result = progressService.completeTask(userId, taskId);
        return new TaskDTO.CompleteResponse(
                result.taskId(),
                result.lessonId(),
                result.examId(),
                result.completed(),
                result.firstCompletion(),
                result.xpGranted(),
                result.coinGranted()
        );
    }

    @PutMapping("/{taskId}/rewards")
    public TaskDTO.RewardResponse updateTaskRewards(
            @PathVariable("taskId") UUID taskId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody TaskDTO.UpdateRewardsRequest request
    ) {
        authService.requireAdminUserIdFromAccessHeader(authorizationHeader);
        ProgressService.TaskRewardResult result = progressService.updateTaskRewards(
                taskId,
                request.xpReward(),
                request.coinReward()
        );
        return new TaskDTO.RewardResponse(
                result.taskId(),
                result.lessonId(),
                result.examId(),
                result.xpReward(),
                result.coinReward()
        );
    }
}
