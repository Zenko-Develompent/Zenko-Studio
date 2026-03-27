package com.hackathon.edu.seed;

import com.hackathon.edu.config.AppSeedProperties;
import com.hackathon.edu.entity.CourseEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.repository.CourseRepository;
import com.hackathon.edu.repository.LessonRepository;
import com.hackathon.edu.repository.ModuleRepository;
import com.hackathon.edu.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeedRunner implements CommandLineRunner {
    private final AppSeedProperties props;
    private final AuthService authService;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    @Override
    public void run(String... args) {
        if (!props.isEnabled()) {
            return;
        }
        seed();
    }

    @Transactional
    void seed() {
        var admin = props.getAdmin();
        authService.upsertAdmin(admin.getUsername(), admin.getPassword(), admin.getAge());

        var courseProps = props.getCourse();
        CourseEntity course = courseRepository.findByNameIgnoreCase(courseProps.getName())
                .orElseGet(() -> {
                    CourseEntity entity = new CourseEntity();
                    entity.setName(courseProps.getName());
                    return courseRepository.save(entity);
                });
        course.setDescription(courseProps.getDescription());
        course.setCategory(courseProps.getCategory());
        course = courseRepository.save(course);

        ModuleEntity module = moduleRepository.findByCourse_CourseIdAndNameIgnoreCase(course.getCourseId(), courseProps.getModuleName())
                .orElse(null);
        if (module == null) {
            ModuleEntity entity = new ModuleEntity();
            entity.setCourse(course);
            entity.setName(courseProps.getModuleName());
            entity.setDescription("Seeded module");
            module = moduleRepository.save(entity);
        }
        if (module.getCourse() == null) {
            module.setCourse(course);
        }
        module = moduleRepository.save(module);

        ensureLesson(module, "Lesson 1", "Seeded lesson 1", "Lesson 1 body", 10);
        ensureLesson(module, "Lesson 2", "Seeded lesson 2", "Lesson 2 body", 20);

        log.info("Seed done: courseId={}, moduleId={}", course.getCourseId(), module.getModuleId());
    }

    private void ensureLesson(ModuleEntity module, String name, String description, String body, int xp) {
        LessonEntity lesson = lessonRepository.findByModuleIdAndNameIgnoreCase(module.getModuleId(), name)
                .orElseGet(() -> {
                    LessonEntity entity = new LessonEntity();
                    entity.setName(name);
                    return entity;
                });

        lesson.setDescription(description);
        lesson.setXp(xp);

        // `LessonEntity.body` stores a path to a .md/.txt file; keep it null for seeded lessons.
        if (lesson.getBody() == null || lesson.getBody().isBlank()) {
            lesson.setBody(null);
        }

        if (lesson.getLessonId() == null) {
            module.getLessons().add(lesson);
            moduleRepository.save(module);
        } else {
            lessonRepository.save(lesson);
        }
    }
}
