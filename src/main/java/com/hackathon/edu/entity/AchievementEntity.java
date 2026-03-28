package com.hackathon.edu.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
@Table(name = "achievement")
@Getter
@Setter
public class AchievementEntity {
    @Id
    @Column(name = "achievement_id", nullable = false, updatable = false)
    private UUID achievementId;

    @Column(name = "code", unique = true, length = 64)
    private String code;

    @Column(unique = true, nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "icon", length = 255)
    private String icon;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "achievement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AchievementUserEntity> users = new ArrayList<>();

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (achievementId == null) {
            achievementId = UUID.randomUUID();
        }
        if (code == null || code.isBlank()) {
            code = achievementId.toString();
        }
        if (icon == null || icon.isBlank()) {
            icon = "🏆";
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
