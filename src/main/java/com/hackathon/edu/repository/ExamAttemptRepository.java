package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ExamAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttemptEntity, UUID> {
    Optional<ExamAttemptEntity> findByExam_ExamIdAndUserId(UUID examId, UUID userId);

    boolean existsByExam_ExamIdAndUserIdAndCompletedTrue(UUID examId, UUID userId);
}
