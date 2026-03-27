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

    @Query("""
            select count(l)
            from ModuleEntity m
            join m.lessons l
            where m.moduleId = :moduleId
            """)
    long countByModuleId(@Param("moduleId") UUID moduleId);

    @Query("""
            select count(l)
            from ModuleEntity m
            join m.lessons l
            where m.course.courseId = :courseId
            """)
    long countByCourseId(@Param("courseId") UUID courseId);

    @Query("""
            select m.moduleId as moduleId, count(l) as cnt
            from ModuleEntity m
            left join m.lessons l
            where m.course.courseId = :courseId
            group by m.moduleId
            """)
    List<ModuleLessonCount> countLessonsByModuleForCourse(@Param("courseId") UUID courseId);

    @EntityGraph(attributePaths = {"quiz", "task"})
    Optional<LessonEntity> findWithRelationsByLessonId(UUID lessonId);

    @Query("""
            select l
            from ModuleEntity m
            join m.lessons l
            where m.moduleId = :moduleId and lower(l.name) = lower(:name)
            """)
    Optional<LessonEntity> findByModuleIdAndNameIgnoreCase(@Param("moduleId") UUID moduleId, @Param("name") String name);
}
