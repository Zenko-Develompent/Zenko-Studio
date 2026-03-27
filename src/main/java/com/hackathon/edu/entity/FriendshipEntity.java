package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "friendship",
        uniqueConstraints = @UniqueConstraint(name = "uq_friendship_user_friend", columnNames = {"user_id", "friend_user_id"})
)
@Getter
@Setter
public class FriendshipEntity {
    @Id
    @Column(name = "friendship_id", nullable = false, updatable = false)
    private UUID friendshipId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "friend_user_id", nullable = false)
    private UUID friendUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (friendshipId == null) {
            friendshipId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
