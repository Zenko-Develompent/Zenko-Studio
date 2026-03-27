package com.hackathon.edu.controller;

import com.hackathon.edu.dto.course.CourseDTO;
import com.hackathon.edu.dto.progress.ProgressDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.CourseService;
import com.hackathon.edu.service.ProgressQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import com.hackathon.edu.dto.course.CourseDTO.CreateCourseRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private final ProgressQueryService progressQueryService;
    private final AuthService authService;

    @GetMapping("/courses")
    public CourseDTO.CourseListResponse courses(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return courseService.listCourses(page, size);
    }

    @GetMapping("/courses/{courseId}")
    public CourseDTO.CourseDetailResponse course(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return courseService.getCourse(courseId, resolveOptionalUserId(authorizationHeader));
    }

    @GetMapping("/courses/{courseId}/modules")
    public CourseDTO.CourseModulesResponse courseModules(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return courseService.getCourseModules(courseId, resolveOptionalUserId(authorizationHeader));
    }

    @GetMapping("/courses/{courseId}/tree")
    public CourseDTO.CourseTreeResponse courseTree(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return courseService.getCourseTree(courseId, resolveOptionalUserId(authorizationHeader));
    }

    @GetMapping("/courses/{courseId}/progress")
    public ProgressDTO.ProgressResponse courseProgress(
            @PathVariable("courseId") UUID courseId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return progressQueryService.getCourseProgress(userId, courseId);
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseDTO.CourseDetailResponse> createLesson(
            @Valid @RequestBody CreateCourseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(request));
    }
    //zenkos
    private UUID resolveOptionalUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        return authService.requireUserIdFromAccessHeader(authorizationHeader);
    }
}

