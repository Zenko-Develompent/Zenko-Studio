package com.hackathon.edu.repository;

import com.hackathon.edu.entity.FriendshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, UUID> {
    boolean existsByUserIdAndFriendUserId(UUID userId, UUID friendUserId);

    long countByUserId(UUID userId);

    Optional<FriendshipEntity> findByUserIdAndFriendUserId(UUID userId, UUID friendUserId);

    List<FriendshipEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByUserIdAndFriendUserId(UUID userId, UUID friendUserId);
}
