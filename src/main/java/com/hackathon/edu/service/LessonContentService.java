package com.hackathon.edu.service;

import com.hackathon.edu.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.regex.Pattern;

@Service
public class LessonContentService {
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$");
    private static final String API_NAMED_PREFIX = "/api/lesson-content/";
    private final Path lessonsRoot;

    public LessonContentService(@Value("${app.content.lessons-root:}") String lessonsRoot) {
        String raw = lessonsRoot == null ? "" : lessonsRoot.trim();
        Path root = raw.isEmpty() ? Paths.get("lesson-content") : Paths.get(raw);
        this.lessonsRoot = root.toAbsolutePath().normalize();
    }

    public String readRawMarkdown(String bodyPath) {
        if (bodyPath == null || bodyPath.isBlank()) {
            return null;
        }

        bodyPath = normalizeBodyReference(bodyPath);
        Path resolvedPath = resolveBodyPath(bodyPath);
        Path markdownFile = resolveTextFile(resolvedPath);
        try {
            return stripBom(Files.readString(markdownFile, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "lesson_content_read_failed");
        }
    }

    public String storeLessonBody(UUID lessonId, MultipartFile file) {
        if (lessonId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "bad_request");
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_empty");
        }

        String ext = fileExtension(file.getOriginalFilename());
        if (!isSupportedExtension(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_extension_invalid");
        }

        try {
            Path lessonsDir = lessonsRoot.resolve("lessons");
            Files.createDirectories(lessonsDir);
            String fileName = lessonId + ext;
            Path target = lessonsDir.resolve(fileName).normalize();
            if (!target.startsWith(lessonsRoot)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_path_invalid");
            }

            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return lessonsRoot.relativize(target).toString().replace('\\', '/');
        } catch (ApiException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "lesson_content_write_failed");
        }
    }

    public String storeNamedMarkdown(String nameRaw, MultipartFile file) {
        String name = normalizeNamed(nameRaw);
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_empty");
        }

        try {
            Path dir = lessonsRoot.resolve("named");
            Files.createDirectories(dir);

            Path target = dir.resolve(name + ".md").normalize();
            if (!target.startsWith(lessonsRoot)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_path_invalid");
            }
            if (Files.exists(target)) {
                throw new ApiException(HttpStatus.CONFLICT, "lesson_content_name_taken");
            }

            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return lessonsRoot.relativize(target).toString().replace('\\', '/');
        } catch (ApiException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "lesson_content_write_failed");
        }
    }

    public String toNamedMarkdownBodyLink(String nameRaw) {
        return API_NAMED_PREFIX + normalizeNamed(nameRaw);
    }

    public List<String> listNamedMarkdownBodyLinks() {
        Path dir = lessonsRoot.resolve("named").normalize();
        if (!dir.startsWith(lessonsRoot) || !Files.exists(dir) || !Files.isDirectory(dir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName() == null ? "" : path.getFileName().toString())
                    .filter(fileName -> fileName.toLowerCase(Locale.ROOT).endsWith(".md"))
                    .map(fileName -> fileName.substring(0, fileName.length() - 3))
                    .filter(name -> SAFE_NAME.matcher(name).matches())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .map(name -> name)
                    .toList();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "lesson_content_list_failed");
        }
    }

    public ResolvedLessonFile resolveNamedMarkdown(String nameRaw) {
        try {
            String name = normalizeNamed(nameRaw);
            return resolveForDownload("named/" + name + ".md");
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.BAD_REQUEST) {
                throw new ApiException(HttpStatus.NOT_FOUND, "lesson_content_not_found");
            }
            throw ex;
        }
    }

    public ResolvedLessonFile resolveForDownload(String bodyPath) {
        if (bodyPath == null || bodyPath.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "lesson_content_not_found");
        }
        bodyPath = normalizeBodyReference(bodyPath);
        Path resolvedPath = resolveBodyPath(bodyPath);
        Path filePath = resolveTextFile(resolvedPath);
        String name = filePath.getFileName() == null ? "lesson" : filePath.getFileName().toString();
        return new ResolvedLessonFile(filePath, name, contentType(filePath));
    }

    public void deleteLessonBodyIfExists(String bodyPath) {
        if (bodyPath == null || bodyPath.isBlank()) {
            return;
        }
        bodyPath = normalizeBodyReference(bodyPath);
        try {
            Path resolvedPath = resolveBodyPath(bodyPath);
            if (Files.isRegularFile(resolvedPath) && isSupportedFile(resolvedPath)) {
                Files.deleteIfExists(resolvedPath);
            }
        } catch (Exception ignored) {
        }
    }

    private String normalizeBodyReference(String bodyPath) {
        String value = bodyPath.trim();
        if (value.startsWith(API_NAMED_PREFIX)) {
            String name = value.substring(API_NAMED_PREFIX.length());
            return "named/" + normalizeNamed(name) + ".md";
        }
        return value;
    }

    private String normalizeNamed(String nameRaw) {
        if (nameRaw == null || nameRaw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_name_invalid");
        }
        String name = nameRaw.trim();
        if (name.toLowerCase(Locale.ROOT).endsWith(".md")) {
            name = name.substring(0, name.length() - 3);
        }
        if (!SAFE_NAME.matcher(name).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_name_invalid");
        }
        return name;
    }

    public record ResolvedLessonFile(Path path, String filename, String contentType) {
    }

    private Path resolveBodyPath(String bodyPath) {
        Path sourcePath = Paths.get(bodyPath.trim());
        Path resolvedPath = sourcePath.isAbsolute()
                ? sourcePath.normalize()
                : lessonsRoot.resolve(sourcePath).normalize();

        if (!resolvedPath.startsWith(lessonsRoot)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_path_invalid");
        }
        return resolvedPath;
    }

    private Path resolveTextFile(Path resolvedPath) {
        if (!Files.exists(resolvedPath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "lesson_content_not_found");
        }
        if (Files.isDirectory(resolvedPath)) {
            return resolveFromDirectory(resolvedPath);
        }
        if (!Files.isRegularFile(resolvedPath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "lesson_content_not_found");
        }
        if (!isSupportedFile(resolvedPath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lesson_content_not_text");
        }
        return resolvedPath;
    }

    private Path resolveFromDirectory(Path directoryPath) {
        try (Stream<Path> stream = Files.list(directoryPath)) {
            List<Path> markdownFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .sorted(Comparator
                            .comparing((Path path) -> scoreExtension(path), Comparator.reverseOrder())
                            .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
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

    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        return isSupportedExtension(fileExtension(fileName));
    }

    private boolean isSupportedExtension(String ext) {
        if (ext == null) {
            return false;
        }
        String value = ext.toLowerCase(Locale.ROOT);
        return value.equals(".md") || value.equals(".txt");
    }

    private int scoreExtension(Path path) {
        String ext = fileExtension(path.getFileName() == null ? "" : path.getFileName().toString());
        if (ext == null) {
            return 0;
        }
        String value = ext.toLowerCase(Locale.ROOT);
        if (value.equals(".md")) {
            return 2;
        }
        if (value.equals(".txt")) {
            return 1;
        }
        return 0;
    }

    private String fileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        return fileName.substring(dot);
    }

    private String stripBom(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private String contentType(Path path) {
        String ext = fileExtension(path.getFileName() == null ? "" : path.getFileName().toString());
        if (ext != null && ext.equalsIgnoreCase(".md")) {
            return "text/markdown; charset=utf-8";
        }
        return "text/plain; charset=utf-8";
    }
}
