package com.hackathon.edu.repository;

import com.hackathon.edu.entity.LessonEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<LessonEntity, UUID> {
    interface ModuleLessonCount {
        UUID getModuleId();

        long getCnt();
    }

    long countByModule_ModuleId(UUID moduleId);

    @Query("select count(l) from LessonEntity l where l.module.course.courseId = :courseId")
    long countByCourseId(@Param("courseId") UUID courseId);

    @Query("""
            select l.module.moduleId as moduleId, count(l) as cnt
            from LessonEntity l
            where l.module.course.courseId = :courseId
            group by l.module.moduleId
            """)
    List<ModuleLessonCount> countLessonsByModuleForCourse(@Param("courseId") UUID courseId);

    @EntityGraph(attributePaths = {"module", "quiz", "task"})
    Optional<LessonEntity> findWithRelationsByLessonId(UUID lessonId);

    Optional<LessonEntity> findByModule_ModuleIdAndNameIgnoreCase(UUID moduleId, String name);
}
