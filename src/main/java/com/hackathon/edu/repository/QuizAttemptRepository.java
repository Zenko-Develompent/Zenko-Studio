package com.hackathon.edu.repository;

import com.hackathon.edu.entity.QuizAttemptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QuizAttemptEntity> findByQuiz_QuizIdAndUserId(UUID quizId, UUID userId);
}
