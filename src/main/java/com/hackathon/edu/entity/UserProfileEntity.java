package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@Setter
public class UserProfileEntity {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "display_name", nullable = false)
    private String displayName = "";

    @Column(name = "bio", nullable = false)
    private String bio = "";

    @Column(name = "location", nullable = false)
    private String location = "";

    @Column(name = "website", nullable = false)
    private String website = "";

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "avatar_rev", nullable = false)
    private int avatarRev;

    @Column(name = "banner_rev", nullable = false)
    private int bannerRev;

    @Column(name = "profile_theme")
    private String profileTheme;

    @Column(name = "custom_theme_color1")
    private String customThemeColor1;

    @Column(name = "custom_theme_color2")
    private String customThemeColor2;

    @Column(name = "wallpaper_rev", nullable = false)
    private int wallpaperRev;

    @Column(name = "user_status")
    private String userStatus;

    @Column(name = "custom_status")
    private String customStatus;

    @Column(name = "social_links", nullable = false, columnDefinition = "text")
    private String socialLinks = "[]";

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (socialLinks == null || socialLinks.isBlank()) {
            socialLinks = "[]";
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
