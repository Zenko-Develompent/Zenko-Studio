package com.hackathon.edu.service;

import com.hackathon.edu.dto.shop.ShopDTO;
import com.hackathon.edu.entity.ShopPurchaseEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.ShopPurchaseRepository;
import com.hackathon.edu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopService {
    private final ShopCatalogService shopCatalogService;
    private final ShopPurchaseRepository shopPurchaseRepository;
    private final UserRepository userRepository;
    private final AchievementProgressService achievementProgressService;

    public ShopDTO.ItemsResponse getItems() {
        List<ShopDTO.Item> items = shopCatalogService.list().stream()
                .map(item -> new ShopDTO.Item(
                        item.id(),
                        item.name(),
                        item.description(),
                        item.icon(),
                        Math.max(0, item.price())
                ))
                .toList();
        return new ShopDTO.ItemsResponse(items);
    }

    @Transactional(readOnly = true)
    public ShopDTO.StateResponse getState(UUID userId) {
        UserEntity user = requireUser(userId);
        return new ShopDTO.StateResponse(
                user.getUserId(),
                safeInt(user.getCoins()),
                safeInt(user.getXpBoostCharges())
        );
    }

    @Transactional
    public ShopDTO.PurchaseResponse purchase(UUID userId, String itemIdRaw) {
        String itemId = normalizeItemId(itemIdRaw);
        if (itemId == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "shop_item_not_found");
        }

        ShopCatalogService.ShopItemDefinition item = shopCatalogService.list().stream()
                .filter(def -> itemId.equals(def.id()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "shop_item_not_found"));

        UserEntity user = requireUser(userId);
        int price = Math.max(0, item.price());
        int coins = safeInt(user.getCoins());
        if (coins < price) {
            throw new ApiException(HttpStatus.CONFLICT, "insufficient_coins");
        }

        user.setCoins(coins - price);
        applyItemEffect(user, item);
        userRepository.save(user);

        ShopPurchaseEntity purchase = new ShopPurchaseEntity();
        purchase.setUserId(userId);
        purchase.setItemId(item.id());
        purchase.setItemName(item.name());
        purchase.setItemPrice(price);
        purchase.setShopGroup(normalizeGroup(item.shopGroup()));
        purchase.setCountsAsUpgrade(item.countsAsUpgrade());
        shopPurchaseRepository.save(purchase);

        achievementProgressService.evaluateForUser(userId);

        return new ShopDTO.PurchaseResponse(
                user.getUserId(),
                item.id(),
                item.name(),
                price,
                safeInt(user.getCoins()),
                safeInt(user.getXpBoostCharges())
        );
    }

    private void applyItemEffect(UserEntity user, ShopCatalogService.ShopItemDefinition item) {
        if (item.xpBoostPercent() > 0 && item.quizTaskCharges() > 0) {
            int next = safeInt(user.getXpBoostCharges()) + item.quizTaskCharges();
            user.setXpBoostCharges(Math.max(0, next));
        }
    }

    private String normalizeItemId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeGroup(String raw) {
        if (raw == null || raw.isBlank()) {
            return "default";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found"));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
