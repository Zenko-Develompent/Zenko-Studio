package com.hackathon.edu.repository;

import com.hackathon.edu.entity.CourseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {
    Page<CourseEntity> findAllByOrderByNameAsc(Pageable pageable);

    Optional<CourseEntity> findByNameIgnoreCase(String name);

    boolean existsByCategoryIgnoreCase(String category);

    @EntityGraph(attributePaths = {"modules", "modules.exam"})
    Optional<CourseEntity> findWithModulesByCourseId(UUID courseId);

    Optional<CourseEntity> findWithTreeByCourseId(UUID courseId);
}
