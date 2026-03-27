package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "private_chat",
        uniqueConstraints = @UniqueConstraint(name = "uq_private_chat_pair", columnNames = {"user_low_id", "user_high_id"})
)
@Getter
@Setter
public class PrivateChatEntity {
    @Id
    @Column(name = "chat_id", nullable = false, updatable = false)
    private UUID chatId;

    @Column(name = "user_low_id", nullable = false)
    private UUID userLowId;

    @Column(name = "user_high_id", nullable = false)
    private UUID userHighId;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (chatId == null) {
            chatId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
