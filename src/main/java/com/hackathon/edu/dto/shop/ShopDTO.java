package com.hackathon.edu.dto.shop;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public final class ShopDTO {
    private ShopDTO() {
    }

    public record Item(
            String itemId,
            String name,
            String description,
            String icon,
            int price
    ) {
    }

    public record ItemsResponse(
            List<Item> items
    ) {
    }

    public record PurchaseRequest(
            @NotBlank
            String itemId
    ) {
    }

    public record PurchaseResponse(
            UUID userId,
            String itemId,
            String itemName,
            int price,
            int coinsAfterPurchase,
            int xpBoostCharges
    ) {
    }

    public record StateResponse(
            UUID userId,
            int coins,
            int xpBoostCharges
    ) {
    }
}
