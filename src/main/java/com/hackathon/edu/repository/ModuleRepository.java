package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ModuleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleRepository extends JpaRepository<ModuleEntity, UUID> {
    long countByCourse_CourseId(UUID courseId);

    @EntityGraph(attributePaths = {"course", "exam"})
    List<ModuleEntity> findByCourse_CourseIdOrderByNameAsc(UUID courseId);

    @EntityGraph(attributePaths = {"course", "exam"})
    Optional<ModuleEntity> findWithCourseAndExamByModuleId(UUID moduleId);

    @EntityGraph(attributePaths = {"lessons", "lessons.quiz", "lessons.task"})
    Optional<ModuleEntity> findWithLessonsByModuleId(UUID moduleId);
}
