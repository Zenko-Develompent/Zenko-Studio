package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "parent_child_link",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_parent_child_link_parent_child",
                columnNames = {"parent_user_id", "child_user_id"}
        )
)
@Getter
@Setter
public class ParentChildLinkEntity {
    @Id
    @Column(name = "link_id", nullable = false, updatable = false)
    private UUID linkId;

    @Column(name = "parent_user_id", nullable = false)
    private UUID parentUserId;

    @Column(name = "child_user_id", nullable = false)
    private UUID childUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (linkId == null) {
            linkId = UUID.randomUUID();
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
