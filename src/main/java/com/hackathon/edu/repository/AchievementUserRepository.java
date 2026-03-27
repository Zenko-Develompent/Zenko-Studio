package com.hackathon.edu.repository;

import com.hackathon.edu.entity.AchievementUserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AchievementUserRepository extends JpaRepository<AchievementUserEntity, UUID> {
    @EntityGraph(attributePaths = {"achievement"})
    List<AchievementUserEntity> findByUser_UserIdOrderByCreatedAtAsc(UUID userId);
}

