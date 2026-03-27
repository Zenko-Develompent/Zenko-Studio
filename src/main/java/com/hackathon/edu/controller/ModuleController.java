package com.hackathon.edu.controller;

import com.hackathon.edu.dto.module.ModuleDTO;
import com.hackathon.edu.dto.progress.ProgressDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.ModuleService;
import com.hackathon.edu.service.ProgressQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {
    private final ModuleService moduleService;
    private final ProgressQueryService progressQueryService;
    private final AuthService authService;

    @GetMapping
    public ModuleDTO.ModuleListResponse modulesByCourse(@RequestParam("courseId") UUID courseId) {
        return moduleService.listModulesByCourse(courseId);
    }

    @GetMapping("/{moduleId}")
    public ModuleDTO.ModuleDetailResponse module(@PathVariable("moduleId") UUID moduleId) {
        return moduleService.getModule(moduleId);
    }

    @GetMapping("/{moduleId}/lessons")
    public ModuleDTO.ModuleLessonsResponse moduleLessons(@PathVariable("moduleId") UUID moduleId) {
        return moduleService.getModuleLessons(moduleId);
    }

    @GetMapping("/{moduleId}/exam")
    public ModuleDTO.ModuleExamResponse moduleExam(@PathVariable("moduleId") UUID moduleId) {
        return moduleService.getModuleExam(moduleId);
    }

    @GetMapping("/{moduleId}/progress")
    public ProgressDTO.ProgressResponse moduleProgress(
            @PathVariable("moduleId") UUID moduleId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return progressQueryService.getModuleProgress(userId, moduleId);
    }

    @PostMapping
    public ResponseEntity<ModuleDTO.ModuleDetailResponse> createModule(@Valid @RequestBody ModuleDTO.ModuleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moduleService.createModule(request));
    }

    @PutMapping("/{moduleId}")
    public ModuleDTO.ModuleDetailResponse updateModule(
            @PathVariable("moduleId") UUID moduleId,
            @Valid @RequestBody ModuleDTO.ModuleUpdateRequest request
    ) {
        return moduleService.updateModule(moduleId, request);
    }

    @DeleteMapping("/{moduleId}")
    public ResponseEntity<Void> deleteModule(@PathVariable("moduleId") UUID moduleId) {
        moduleService.deleteModule(moduleId);
        return ResponseEntity.noContent().build();
    }
}
