package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ExamQuestionProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamQuestionProgressRepository extends JpaRepository<ExamQuestionProgressEntity, UUID> {
    Optional<ExamQuestionProgressEntity> findByQuestion_QuestIdAndUserId(UUID questionId, UUID userId);

    long countByQuestion_Exam_ExamIdAndUserId(UUID examId, UUID userId);
}
