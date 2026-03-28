package com.hackathon.edu.repository;

import com.hackathon.edu.entity.TaskAttemptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface TaskAttemptRepository extends JpaRepository<TaskAttemptEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TaskAttemptEntity> findByTask_TasksIdAndUserId(UUID taskId, UUID userId);

    boolean existsByTask_TasksIdAndUserIdAndCompletedTrue(UUID taskId, UUID userId);

    long countByTask_Exam_ExamIdAndUserIdAndCompletedTrue(UUID examId, UUID userId);
}
