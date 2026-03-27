package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "activity_event",
        indexes = {
                @Index(name = "idx_activity_event_created_at", columnList = "created_at"),
                @Index(name = "idx_activity_event_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@Setter
public class ActivityEventEntity {
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "activity_score", nullable = false)
    private Integer activityScore = 0;

    @Column(name = "xp_granted", nullable = false)
    private Integer xpGranted = 0;

    @Column(name = "coin_granted", nullable = false)
    private Integer coinGranted = 0;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "quiz_id")
    private UUID quizId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "exam_id")
    private UUID examId;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (activityScore == null) {
            activityScore = 0;
        }
        if (xpGranted == null) {
            xpGranted = 0;
        }
        if (coinGranted == null) {
            coinGranted = 0;
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
