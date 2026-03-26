package com.hackathon.edu.repository;

import com.hackathon.edu.entity.RefreshTokenEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    List<RefreshTokenEntity> findByUserIdAndRevokedAtIsNull(UUID userId);

    @Transactional
    @Modifying
    @Query("update RefreshTokenEntity t set t.replacedBy = :newId where t.tokenId = :oldId")
    int markReplaced(@Param("oldId") UUID oldId, @Param("newId") UUID newId);

    @Transactional
    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") OffsetDateTime now);
}
