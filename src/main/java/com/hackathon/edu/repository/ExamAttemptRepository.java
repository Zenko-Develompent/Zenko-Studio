package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ExamAttemptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttemptEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExamAttemptEntity> findByExam_ExemIdAndUserId(UUID examId, UUID userId);

    boolean existsByExam_ExemIdAndUserIdAndCompletedTrue(UUID examId, UUID userId);
}
