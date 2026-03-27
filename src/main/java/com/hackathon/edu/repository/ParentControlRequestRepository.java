package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ParentControlRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParentControlRequestRepository extends JpaRepository<ParentControlRequestEntity, UUID> {
    boolean existsByParentUserIdAndChildUserIdAndStatus(UUID parentUserId, UUID childUserId, String status);

    List<ParentControlRequestEntity> findByParentUserIdAndStatusOrderByCreatedAtDesc(UUID parentUserId, String status);

    List<ParentControlRequestEntity> findByChildUserIdAndStatusOrderByCreatedAtDesc(UUID childUserId, String status);
}
