package com.hackathon.edu.controller;

import com.hackathon.edu.dto.achievement.AchievementDTO;
import com.hackathon.edu.service.AchievementQueryService;
import com.hackathon.edu.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {
    private final AchievementQueryService achievementQueryService;
    private final AuthService authService;

    @GetMapping
    public AchievementDTO.AchievementListResponse list(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = null;
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        }
        return achievementQueryService.list(userId);
    }
}
