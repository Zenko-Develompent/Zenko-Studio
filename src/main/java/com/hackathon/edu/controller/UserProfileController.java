package com.hackathon.edu.controller;

import com.hackathon.edu.dto.ProfileDTO;
import com.hackathon.edu.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {
    private final AuthService authService;

    @GetMapping("/{userId}/profile")
    public ProfileDTO.PublicProfileResponse publicProfile(
            @PathVariable("userId") UUID userId
    ) {
        return authService.getPublicProfile(userId);
    }
}
