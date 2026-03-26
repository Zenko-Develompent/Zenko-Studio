package com.hackathon.edu.repository;

import com.hackathon.edu.entity.QuestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestRepository extends JpaRepository<QuestEntity, UUID> {
    List<QuestEntity> findByQuiz_QuizIdOrderByCreatedAtAsc(UUID quizId);

    List<QuestEntity> findByExam_ExemIdOrderByCreatedAtAsc(UUID examId);
}

