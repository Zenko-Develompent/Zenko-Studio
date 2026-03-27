package com.hackathon.edu.repository;

import com.hackathon.edu.entity.AchievementUserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AchievementUserRepository extends JpaRepository<AchievementUserEntity, UUID> {
    @EntityGraph(attributePaths = {"achievement"})
    List<AchievementUserEntity> findByUser_UserIdOrderByCreatedAtAsc(UUID userId);

    boolean existsByUser_UserIdAndAchievement_Code(UUID userId, String achievementCode);

    @Query("""
            select au.achievement.code
            from AchievementUserEntity au
            where au.user.userId = :userId
            """)
    List<String> findAchievementCodesByUserId(@Param("userId") UUID userId);
}

