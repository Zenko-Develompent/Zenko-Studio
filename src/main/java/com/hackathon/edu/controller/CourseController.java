package com.hackathon.edu.controller;

import com.hackathon.edu.dto.course.CourseDTO;
import com.hackathon.edu.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import com.hackathon.edu.dto.course.CourseDTO.CreateCourseRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @GetMapping("/courses")
    public CourseDTO.CourseListResponse courses(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return courseService.listCourses(page, size);
    }

    @GetMapping("/courses/{courseId}")
    public CourseDTO.CourseDetailResponse course(@PathVariable("courseId") UUID courseId) {
        return courseService.getCourse(courseId);
    }

    @GetMapping("/courses/{courseId}/modules")
    public CourseDTO.CourseModulesResponse courseModules(@PathVariable("courseId") UUID courseId) {
        return courseService.getCourseModules(courseId);
    }

    @GetMapping("/courses/{courseId}/tree")
    public CourseDTO.CourseTreeResponse courseTree(@PathVariable("courseId") UUID courseId) {
        return courseService.getCourseTree(courseId);
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseDTO.CourseDetailResponse> createLesson(
            @Valid @RequestBody CreateCourseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(request));
    }
}

