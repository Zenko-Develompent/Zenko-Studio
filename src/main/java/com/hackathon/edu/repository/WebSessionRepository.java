package com.hackathon.edu.repository;

import com.hackathon.edu.entity.WebSessionEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebSessionRepository extends JpaRepository<WebSessionEntity, UUID> {
    List<WebSessionEntity> findByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);

    Optional<WebSessionEntity> findBySessionIdAndActiveTrue(UUID sessionId);

    Optional<WebSessionEntity> findBySessionIdAndUserIdAndActiveTrue(UUID sessionId, UUID userId);

    @Transactional
    @Modifying
    @Query("update WebSessionEntity s set s.active = false where s.sessionId = :sessionId and s.active = true")
    int deactivate(@Param("sessionId") UUID sessionId);

    @Transactional
    @Modifying
    @Query("update WebSessionEntity s set s.active = false where s.sessionId = :sessionId and s.userId = :userId and s.active = true")
    int deactivateForUser(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId);

    @Transactional
    @Modifying
    @Query("update WebSessionEntity s set s.lastActivityAt = :now where s.sessionId = :sessionId and s.active = true")
    int touch(@Param("sessionId") UUID sessionId, @Param("now") OffsetDateTime now);
}
