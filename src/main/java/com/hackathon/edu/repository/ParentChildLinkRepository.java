package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ParentChildLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentChildLinkRepository extends JpaRepository<ParentChildLinkEntity, UUID> {
    boolean existsByParentUserIdAndChildUserId(UUID parentUserId, UUID childUserId);

    Optional<ParentChildLinkEntity> findByParentUserIdAndChildUserId(UUID parentUserId, UUID childUserId);

    List<ParentChildLinkEntity> findByParentUserIdOrderByCreatedAtDesc(UUID parentUserId);
}
