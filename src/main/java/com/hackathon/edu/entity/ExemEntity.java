package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exam")
@Getter
@Setter
public class ExamEntity {
    @Id
    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, unique = true, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ModuleEntity module;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestEntity> quests = new ArrayList<>();

    @OneToMany(mappedBy = "exam")
    private List<TasksEntity> tasks = new ArrayList<>();

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
        if (examId == null) {
            examId = UUID.randomUUID();
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
