package com.hackathon.edu.service;

import com.hackathon.edu.entity.AchievementEntity;
import com.hackathon.edu.entity.AchievementUserEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.AchievementRepository;
import com.hackathon.edu.repository.AchievementUserRepository;
import com.hackathon.edu.repository.ActivityEventRepository;
import com.hackathon.edu.repository.FriendshipRepository;
import com.hackathon.edu.repository.LessonRepository;
import com.hackathon.edu.repository.ParentChildLinkRepository;
import com.hackathon.edu.repository.ShopPurchaseRepository;
import com.hackathon.edu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementProgressService {
    private static final String A01 = "A01_FIRST_CODE";
    private static final String A02 = "A02_YOUNG_PROGRAMMER";
    private static final String A03 = "A03_CODE_MASTER";
    private static final String A04 = "A04_KNOWLEDGE_SEEKER";
    private static final String A05 = "A05_REPEAT_TO_REMEMBER";
    private static final String A06 = "A06_FOUND_FRIEND";
    private static final String A07 = "A07_COOL_TEAM";
    private static final String A08 = "A08_FRIENDLY_CLASS";
    private static final String A09 = "A09_STAR_SQUAD";
    private static final String A10 = "A10_FAMILY";
    private static final String A11 = "A11_FIRST_WORD";
    private static final String A12 = "A12_CURIOUS";
    private static final String A13 = "A13_SOCIABLE";
    private static final String A14 = "A14_DISCUSSION_LEADER";
    private static final String A15 = "A15_COMMUNITY_VOICE";
    private static final String A16 = "A16_ERROR_IS_EXPERIENCE";
    private static final String A17 = "A17_BUG_LOVER";
    private static final String A19 = "A19_MODEST_GENIUS";
    private static final String A20 = "A20_FIRST_STEPS_IT";
    private static final String A21 = "A21_THIRST_FOR_KNOWLEDGE";
    private static final String A22 = "A22_ERUDITE";
    private static final String A23 = "A23_SAGE";
    private static final String A24 = "A24_LIVING_LEGEND";
    private static final String A25 = "A25_NEWBIE";
    private static final String A26 = "A26_EXPERIENCED_CODER";
    private static final String A27 = "A27_CODE_MASTER_LEVEL";
    private static final String A28 = "A28_EXPERT";
    private static final String A29 = "A29_PRO";
    private static final String A30 = "A30_PROGRAMMING_GURU";
    private static final String A31 = "A31_FRIENDSHIP_AND_KNOWLEDGE";
    private static final String A32 = "A32_LONE_WOLF";
    private static final String A33 = "A33_SOLO_GENIUS";
    private static final String A34 = "A34_POCKET_COINS";
    private static final String A35 = "A35_SOLID_SAFE";
    private static final String A36 = "A36_REAL_RICH";
    private static final String A37 = "A37_MILLIONAIRE";
    private static final String A38 = "A38_OIL_MAGNATE";
    private static final String A39 = "A39_FIRST_PURCHASE";
    private static final String A40 = "A40_SHOPPING_LOVER";
    private static final String A41 = "A41_GENEROUS_INVESTOR";
    private static final String A42 = "A42_COLLECTOR";
    private static final String A43 = "A43_EMPTY_WALLET";
    private static final String A44 = "A44_ECONOMICAL";
    private static final String A45 = "A45_BEAUTIFUL_USELESS";
    private static final String A46 = "A46_KNOWLEDGE_OVER_MONEY";

    private final AchievementCatalogService achievementCatalogService;
    private final ShopCatalogService shopCatalogService;
    private final AchievementRepository achievementRepository;
    private final AchievementUserRepository achievementUserRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final FriendshipRepository friendshipRepository;
    private final ParentChildLinkRepository parentChildLinkRepository;
    private final ActivityEventRepository activityEventRepository;
    private final ShopPurchaseRepository shopPurchaseRepository;
    private final ActivityEventService activityEventService;

    @Transactional
    public void evaluateForUser(UUID userId) {
        UserEntity user = requireUser(userId);
        Set<String> unlockedCodes = new HashSet<>(achievementUserRepository.findAchievementCodesByUserId(userId));
        Stats stats = collectStats(user);

        for (AchievementCatalogService.AchievementDefinition definition : achievementCatalogService.list()) {
            if (unlockedCodes.contains(definition.code())) {
                continue;
            }
            if (!isConditionMet(definition.code(), stats)) {
                continue;
            }
            unlock(user, definition, unlockedCodes);
        }
    }

    @Transactional
    public void onTrigger(UUID userId, String trigger, Map<String, String> context) {
        evaluateForUser(userId);
    }

    private Stats collectStats(UserEntity user) {
        UUID userId = user.getUserId();

        long completedLessons = lessonRepository.countCompletedByUserId(userId);
        long totalLessons = lessonRepository.count();
        long friendCount = friendshipRepository.countByUserId(userId);
        long parentLinksAsChild = parentChildLinkRepository.countByChildUserId(userId);
        long messageCount = activityEventRepository.countByUser_UserIdAndEventType(userId, ActivityEventService.TYPE_MESSAGE_SENT);
        long codeErrorCount = activityEventRepository.countByUser_UserIdAndEventType(userId, ActivityEventService.TYPE_CODE_ERROR);
        long lessonRepeatCount = activityEventRepository.countByUser_UserIdAndEventType(userId, ActivityEventService.TYPE_LESSON_REPEATED);
        long purchaseCount = shopPurchaseRepository.countByUserId(userId);
        long spentCoins = shopPurchaseRepository.sumSpentByUserId(userId);
        long upgradePurchaseCount = shopPurchaseRepository.countByUserIdAndCountsAsUpgradeTrue(userId);
        Set<String> purchasedItemIds = new HashSet<>(shopPurchaseRepository.findDistinctItemIdsByUserId(userId));

        List<ShopCatalogService.ShopItemDefinition> items = shopCatalogService.list();
        int maxItemPrice = items.stream()
                .mapToInt(item -> Math.max(0, item.price()))
                .max()
                .orElse(0);
        Set<String> maxPriceItemIds = items.stream()
                .filter(item -> Math.max(0, item.price()) == maxItemPrice)
                .map(ShopCatalogService.ShopItemDefinition::id)
                .collect(Collectors.toSet());
        boolean purchasedMostExpensiveItem = !maxPriceItemIds.isEmpty()
                && maxPriceItemIds.stream().anyMatch(purchasedItemIds::contains);

        Map<String, Set<String>> allItemsByShopGroup = items.stream()
                .collect(Collectors.groupingBy(
                        item -> normalizeShopGroup(item.shopGroup()),
                        Collectors.mapping(ShopCatalogService.ShopItemDefinition::id, Collectors.toSet())
                ));
        boolean boughtAllItemsInAnyShop = allItemsByShopGroup.values().stream()
                .anyMatch(groupItems -> !groupItems.isEmpty() && purchasedItemIds.containsAll(groupItems));

        return new Stats(
                Math.max(0, safeInt(user.getXp())),
                Math.max(0, safeInt(user.getLevel())),
                Math.max(0, safeInt(user.getCoins())),
                completedLessons,
                totalLessons,
                friendCount,
                parentLinksAsChild,
                messageCount,
                codeErrorCount,
                lessonRepeatCount,
                purchaseCount,
                spentCoins,
                upgradePurchaseCount,
                purchasedMostExpensiveItem,
                boughtAllItemsInAnyShop
        );
    }

    private boolean isConditionMet(String code, Stats stats) {
        return switch (code) {
            case A01 -> stats.completedLessons >= 1;
            case A02 -> stats.completedLessons >= 10;
            case A03 -> stats.completedLessons >= 50;
            case A04 -> stats.totalLessons > 0 && stats.completedLessons >= stats.totalLessons;
            case A05 -> stats.lessonRepeatCount >= 1;
            case A06 -> stats.friendCount >= 1;
            case A07 -> stats.friendCount >= 5;
            case A08 -> stats.friendCount >= 10;
            case A09 -> stats.friendCount >= 25;
            case A10 -> stats.parentLinksAsChild >= 1;
            case A11 -> stats.messageCount >= 1;
            case A12 -> stats.messageCount >= 50;
            case A13 -> stats.messageCount >= 100;
            case A14 -> stats.messageCount >= 500;
            case A15 -> stats.messageCount >= 1000;
            case A16 -> stats.codeErrorCount >= 1;
            case A17 -> stats.codeErrorCount >= 10;
            case A19 -> stats.messageCount >= 500 && stats.friendCount == 0;
            case A20 -> stats.level >= 1;
            case A21 -> stats.xp >= 1000;
            case A22 -> stats.xp >= 10_000;
            case A23 -> stats.xp >= 100_000;
            case A24 -> stats.xp >= 1_000_000;
            case A25 -> stats.level >= 5;
            case A26 -> stats.level >= 10;
            case A27 -> stats.level >= 25;
            case A28 -> stats.level >= 50;
            case A29 -> stats.level >= 75;
            case A30 -> stats.level >= 150;
            case A31 -> stats.level >= 20 && stats.friendCount >= 10;
            case A32 -> stats.level >= 20 && stats.friendCount == 0;
            case A33 -> stats.level > 50 && stats.friendCount == 0;
            case A34 -> stats.coins >= 1_000;
            case A35 -> stats.coins >= 10_000;
            case A36 -> stats.coins >= 100_000;
            case A37 -> stats.coins >= 1_000_000;
            case A38 -> stats.coins >= 10_000_000;
            case A39 -> stats.purchaseCount >= 1;
            case A40 -> stats.purchaseCount >= 100;
            case A41 -> stats.spentCoins >= 50_000;
            case A42 -> stats.boughtAllItemsInAnyShop;
            case A43 -> stats.purchaseCount > 0 && stats.coins == 0;
            case A44 -> stats.coins >= 50_000 && stats.purchaseCount == 0;
            case A45 -> stats.purchasedMostExpensiveItem;
            case A46 -> stats.coins >= 100_000 && stats.upgradePurchaseCount == 0;
            default -> false;
        };
    }

    private void unlock(
            UserEntity user,
            AchievementCatalogService.AchievementDefinition definition,
            Set<String> unlockedCodes
    ) {
        if (achievementUserRepository.existsByUser_UserIdAndAchievement_Code(user.getUserId(), definition.code())) {
            unlockedCodes.add(definition.code());
            return;
        }

        AchievementEntity achievement = achievementRepository.findByCode(definition.code())
                .orElseGet(() -> createAchievement(definition));
        achievement.setName(definition.name());
        achievement.setDescription(definition.description());
        achievement.setIcon(definition.icon());
        achievement = achievementRepository.save(achievement);

        AchievementUserEntity row = new AchievementUserEntity();
        row.setUser(user);
        row.setAchievement(achievement);
        try {
            achievementUserRepository.saveAndFlush(row);
            unlockedCodes.add(definition.code());
            activityEventService.recordAchievementUnlocked(user.getUserId(), achievement.getName());
        } catch (DataIntegrityViolationException ignore) {
            unlockedCodes.add(definition.code());
        }
    }

    private AchievementEntity createAchievement(AchievementCatalogService.AchievementDefinition definition) {
        AchievementEntity entity = new AchievementEntity();
        entity.setCode(definition.code());
        entity.setName(definition.name());
        entity.setDescription(definition.description());
        entity.setIcon(definition.icon());
        return entity;
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found"));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeShopGroup(String raw) {
        if (raw == null || raw.isBlank()) {
            return "default";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private record Stats(
            int xp,
            int level,
            int coins,
            long completedLessons,
            long totalLessons,
            long friendCount,
            long parentLinksAsChild,
            long messageCount,
            long codeErrorCount,
            long lessonRepeatCount,
            long purchaseCount,
            long spentCoins,
            long upgradePurchaseCount,
            boolean purchasedMostExpensiveItem,
            boolean boughtAllItemsInAnyShop
    ) {
    }
}
