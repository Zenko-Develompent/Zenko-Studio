package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class TasksEntity {
    @Id
    @Column(name = "tasks_id", nullable = false, updatable = false)
    private UUID tasksId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exem_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ExemEntity exam;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", unique = true, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private LessonEntity lesson;

    @Column(name = "xp_reward")
    private Integer xpReward = 0;

    @Column(name = "coin_reward")
    private Integer coinReward = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (tasksId == null) {
            tasksId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (xpReward == null) {
            xpReward = 0;
        }
        if (coinReward == null) {
            coinReward = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
