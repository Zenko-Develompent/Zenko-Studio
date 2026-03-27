package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "parent_control_request",
        indexes = {
                @Index(name = "idx_parent_control_request_parent_status", columnList = "parent_user_id, status, created_at"),
                @Index(name = "idx_parent_control_request_child_status", columnList = "child_user_id, status, created_at")
        }
)
@Getter
@Setter
public class ParentControlRequestEntity {
    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "parent_user_id", nullable = false)
    private UUID parentUserId;

    @Column(name = "child_user_id", nullable = false)
    private UUID childUserId;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (requestId == null) {
            requestId = UUID.randomUUID();
        }
        if (status == null || status.isBlank()) {
            status = "PENDING";
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
