package com.hackathon.edu.repository;

import com.hackathon.edu.entity.FriendRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, UUID> {
    boolean existsByRequesterUserIdAndReceiverUserIdAndStatus(UUID requesterUserId, UUID receiverUserId, String status);

    boolean existsByRequesterUserIdAndReceiverUserIdAndStatusIn(
            UUID requesterUserId,
            UUID receiverUserId,
            Collection<String> statuses
    );

    List<FriendRequestEntity> findByReceiverUserIdAndStatusOrderByCreatedAtDesc(UUID receiverUserId, String status);

    List<FriendRequestEntity> findByRequesterUserIdAndStatusOrderByCreatedAtDesc(UUID requesterUserId, String status);
}
