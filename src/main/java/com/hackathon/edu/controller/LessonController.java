package com.hackathon.edu.controller;

import com.hackathon.edu.dto.lesson.LessonDTO;
import com.hackathon.edu.dto.progress.ProgressDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.LessonService;
import com.hackathon.edu.service.ProgressQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;
    private final ProgressQueryService progressQueryService;
    private final AuthService authService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonDTO.LessonDetailResponse> createLesson(
            @RequestParam("moduleId") UUID moduleId,
            @RequestParam("name") String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "xp", required = false) Integer xp,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.createLesson(moduleId, name, description, xp, file));
    }
    //zenkmo
    @GetMapping("/{lessonId}")
    public LessonDTO.LessonDetailResponse lesson(
            @PathVariable("lessonId") UUID lessonId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        return lessonService.getLesson(userId, lessonId);
    }

    @PutMapping("/{lessonId}")
    public LessonDTO.LessonDetailResponse updateLesson(
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody LessonDTO.LessonUpdateRequest request
    ) {
        return lessonService.updateLesson(lessonId, request);
    }

    @PutMapping(value = "/{lessonId}/body", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> replaceLessonBody(
            @PathVariable("lessonId") UUID lessonId,
            @RequestPart("file") MultipartFile file
    ) {
        lessonService.replaceLessonBody(lessonId, file);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{lessonId}/body-link")
    public ResponseEntity<Void> setLessonBodyLink(
            @PathVariable("lessonId") UUID lessonId,
            @Valid @RequestBody LessonDTO.LessonBodyLinkRequest request
    ) {
        lessonService.setLessonBodyLink(lessonId, request.body());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{lessonId}/body")
    public ResponseEntity<Resource> lessonBody(
            @PathVariable("lessonId") UUID lessonId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        var resolved = lessonService.getLessonBodyFile(userId, lessonId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resolved.filename() + "\"")
                .contentType(MediaType.parseMediaType(resolved.contentType()))
                .body(new FileSystemResource(resolved.path()));
    }

    @GetMapping("/{lessonId}/quiz")
    public LessonDTO.LessonQuizResponse lessonQuiz(
            @PathVariable("lessonId") UUID lessonId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        return lessonService.getLessonQuiz(userId, lessonId);
    }

    @GetMapping("/{lessonId}/task")
    public LessonDTO.LessonTaskResponse lessonTask(
            @PathVariable("lessonId") UUID lessonId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        return lessonService.getLessonTask(userId, lessonId);
    }

    @GetMapping("/{lessonId}/progress")
    public ProgressDTO.ProgressResponse lessonProgress(
            @PathVariable("lessonId") UUID lessonId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return progressQueryService.getLessonProgress(userId, lessonId);
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable("lessonId") UUID lessonId) {
        lessonService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveOptionalUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        return authService.requireUserIdFromAccessHeader(authorizationHeader);
    }
}
