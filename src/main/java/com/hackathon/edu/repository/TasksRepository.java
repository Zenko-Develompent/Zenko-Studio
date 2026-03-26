package com.hackathon.edu.repository;

import com.hackathon.edu.entity.TasksEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TasksRepository extends JpaRepository<TasksEntity, UUID> {
    Optional<TasksEntity> findByLesson_LessonId(UUID lessonId);

    List<TasksEntity> findByExam_ExemIdOrderByCreatedAtAsc(UUID examId);
}

