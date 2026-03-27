package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
        name = "exam_question_progress",
        uniqueConstraints = @UniqueConstraint(name = "uq_exam_question_progress_quest_user", columnNames = {"quest_id", "user_id"})
)
@Getter
@Setter
public class ExamQuestionProgressEntity {
    @Id
    @Column(name = "progress_id", nullable = false, updatable = false)
    private UUID progressId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private QuestEntity question;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private OffsetDateTime completedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (progressId == null) {
            progressId = UUID.randomUUID();
        }
        if (completedAt == null) {
            completedAt = now;
        }
    }
}
