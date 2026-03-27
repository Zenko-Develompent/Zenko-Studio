package com.hackathon.edu.repository;

import com.hackathon.edu.entity.AnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<AnswerEntity, UUID> {
    List<AnswerEntity> findByQuest_QuestIdOrderByCreatedAtAsc(UUID questId);

    Optional<AnswerEntity> findByAnswerIdAndQuest_QuestId(UUID answerId, UUID questId);
}

