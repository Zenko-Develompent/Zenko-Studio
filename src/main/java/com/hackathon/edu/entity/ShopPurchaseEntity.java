package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "shop_purchase",
        indexes = {
                @Index(name = "idx_shop_purchase_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_shop_purchase_user_item", columnList = "user_id, item_id")
        }
)
@Getter
@Setter
public class ShopPurchaseEntity {
    @Id
    @Column(name = "purchase_id", nullable = false, updatable = false)
    private UUID purchaseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "item_id", nullable = false, length = 64)
    private String itemId;

    @Column(name = "item_name", nullable = false, length = 120)
    private String itemName;

    @Column(name = "item_price", nullable = false)
    private Integer itemPrice;

    @Column(name = "shop_group", nullable = false, length = 64)
    private String shopGroup;

    @Column(name = "counts_as_upgrade", nullable = false)
    private Boolean countsAsUpgrade;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (purchaseId == null) {
            purchaseId = UUID.randomUUID();
        }
        if (itemPrice == null || itemPrice < 0) {
            itemPrice = 0;
        }
        if (countsAsUpgrade == null) {
            countsAsUpgrade = false;
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
