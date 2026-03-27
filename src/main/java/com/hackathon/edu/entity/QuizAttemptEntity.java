package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "quiz_attempt",
        uniqueConstraints = @UniqueConstraint(name = "uq_quiz_attempt_quiz_user", columnNames = {"quiz_id", "user_id"})
)
@Getter
@Setter
public class QuizAttemptEntity {
    @Id
    @Column(name = "attempt_id", nullable = false, updatable = false)
    private UUID attemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QuizEntity quiz;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "current_question_index", nullable = false)
    private Integer currentQuestionIndex = 0;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "reward_granted", nullable = false)
    private Boolean rewardGranted = false;

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (attemptId == null) {
            attemptId = UUID.randomUUID();
        }
        if (currentQuestionIndex == null || currentQuestionIndex < 0) {
            currentQuestionIndex = 0;
        }
        if (completed == null) {
            completed = false;
        }
        if (rewardGranted == null) {
            rewardGranted = false;
        }
        if (startedAt == null) {
            startedAt = now;
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
