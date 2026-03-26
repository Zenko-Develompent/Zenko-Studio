package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "web_sessions")
@Getter
@Setter
public class WebSessionEntity {
    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (sessionId == null) {
            sessionId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastActivityAt == null) {
            lastActivityAt = now;
        }
        active = true;
    }

    @PreUpdate
    void preUpdate() {
        if (lastActivityAt == null) {
            lastActivityAt = OffsetDateTime.now();
        }
    }
}
