package com.hackathon.edu.controller;

import com.hackathon.edu.dto.module.ModuleDTO;
import com.hackathon.edu.service.ModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {
    private final ModuleService moduleService;

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
}

