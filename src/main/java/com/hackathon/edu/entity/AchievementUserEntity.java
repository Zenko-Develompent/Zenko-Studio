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
@Table(name = "achievement")
@Getter
@Setter
public class AchievementUserEntity {
    @Id
    @Column(name = "achievement_user_id", nullable = false, updatable = false)
    private UUID achievementUserId;

    @Column(name = "achievement_id", nullable = false, updatable = false) // связь с ачивками каскадная
    private AchievementEntity achievementId; // связь с пользователем если удалён удалять запись

    @Column(name = "user_id", nullable = false, updatable = false)
    private UserEntity userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
