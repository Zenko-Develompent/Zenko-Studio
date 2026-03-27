package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ModuleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleRepository extends JpaRepository<ModuleEntity, UUID> {
    long countByCourse_CourseId(UUID courseId);

    @EntityGraph(attributePaths = {"course", "exam"})
    List<ModuleEntity> findByCourse_CourseIdOrderByNameAsc(UUID courseId);

    @EntityGraph(attributePaths = {"course", "exam", "lessons", "lessons.quiz", "lessons.task"})
    @Query("""
            select distinct m
            from ModuleEntity m
            left join m.lessons l
            where m.course.courseId = :courseId
            order by m.name asc, m.moduleId asc
            """)
    List<ModuleEntity> findWithExamAndLessonsByCourseIdOrderByNameAsc(@Param("courseId") UUID courseId);

    Optional<ModuleEntity> findByCourse_CourseIdAndNameIgnoreCase(UUID courseId, String name);

    @Query("""
            select m.course.courseId
            from ModuleEntity m
            join m.lessons l
            where l.lessonId = :lessonId
            """)
    UUID findCourseIdByLessonId(@Param("lessonId") UUID lessonId);

    @EntityGraph(attributePaths = {"course", "exam"})
    Optional<ModuleEntity> findWithCourseAndExamByModuleId(UUID moduleId);

    @EntityGraph(attributePaths = {"lessons", "lessons.quiz", "lessons.task"})
    Optional<ModuleEntity> findWithLessonsByModuleId(UUID moduleId);

    @EntityGraph(attributePaths = {"course", "exam", "lessons", "lessons.quiz", "lessons.task"})
    @Query("""
            select m
            from ModuleEntity m
            join m.lessons l
            where l.lessonId = :lessonId
            """)
    Optional<ModuleEntity> findWithCourseExamAndLessonsByLessonId(@Param("lessonId") UUID lessonId);

    @EntityGraph(attributePaths = {"course", "exam", "lessons", "lessons.quiz", "lessons.task"})
    Optional<ModuleEntity> findWithCourseExamAndLessonsByExam_ExemId(UUID examId);
}
