package com.hackathon.edu.controller;

import com.hackathon.edu.service.LessonContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lesson-content")
@RequiredArgsConstructor
public class LessonContentController {
    private final LessonContentService lessonContentService;

    public record LessonContentListResponse(
            List<LessonContentItem> items
    ) {
    }

    public record LessonContentItem(
            String body
    ) {
    }

    @GetMapping
    public LessonContentListResponse list() {
        List<LessonContentItem> items = lessonContentService.listNamedMarkdownBodyLinks().stream()
                .map(LessonContentItem::new)
                .toList();
        return new LessonContentListResponse(items);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("name") String name,
            @RequestPart("file") MultipartFile file
    ) {
        lessonContentService.storeNamedMarkdown(name, file);
        String body = lessonContentService.toNamedMarkdownBodyLink(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("body", body));
    }

    @GetMapping("/{name}")
    public ResponseEntity<Resource> get(@PathVariable("name") String name) {
        var resolved = lessonContentService.resolveNamedMarkdown(name);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resolved.filename() + "\"")
                .contentType(MediaType.parseMediaType(resolved.contentType()))
                .body(new FileSystemResource(resolved.path()));
    }
}
