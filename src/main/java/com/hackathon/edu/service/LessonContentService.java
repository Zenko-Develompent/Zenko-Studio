package com.hackathon.edu.service;

import com.hackathon.edu.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class LessonContentService {
    private final Path lessonsRoot;

    public LessonContentService(@Value("${app.content.lessons-root:}") String lessonsRoot) {
        String raw = lessonsRoot == null ? "" : lessonsRoot.trim();
        this.lessonsRoot = raw.isEmpty() ? null : Paths.get(raw).toAbsolutePath().normalize();
    }

    public String readRawMarkdown(String bodyPath) {
        if (bodyPath == null || bodyPath.isBlank()) {
            return null;
        }

        Path resolvedPath = resolveBodyPath(bodyPath);
        Path markdownFile = resolveMarkdownFile(resolvedPath);
        try {
            return Files.readString(markdownFile, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "lesson_content_read_failed");
        }
    }

    private Path resolveBodyPath(String bodyPath) {
        Path sourcePath = Paths.get(bodyPath.trim());
        Path resolvedPath = sourcePath.isAbsolute()
                ? sourcePath.normalize()
                : lessonsRoot == null
                ? sourcePath.toAbsolutePath().normalize()
                : lessonsRoot.resolve(sourcePath).normalize();

        if (lessonsRoot != null && !resolvedPath.startsWith(lessonsRoot)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_path_invalid");
        }
        return resolvedPath;
    }

    private Path resolveMarkdownFile(Path resolvedPath) {
        if (!Files.exists(resolvedPath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "lesson_content_not_found");
        }
        if (Files.isDirectory(resolvedPath)) {
            return resolveFromDirectory(resolvedPath);
        }
        if (!Files.isRegularFile(resolvedPath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "lesson_content_not_found");
        }
        if (!isMarkdown(resolvedPath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_not_markdown");
        }
        return resolvedPath;
    }

    private Path resolveFromDirectory(Path directoryPath) {
        try (Stream<Path> stream = Files.list(directoryPath)) {
            List<Path> markdownFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isMarkdown)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();

            if (markdownFiles.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "lesson_content_not_found");
            }
            if (markdownFiles.size() > 1) {
                throw new ApiException(HttpStatus.CONFLICT, "lesson_content_ambiguous");
            }
            return markdownFiles.get(0);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "lesson_content_read_failed");
        }
    }

    private boolean isMarkdown(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        return fileName.toLowerCase(Locale.ROOT).endsWith(".md");
    }
}
