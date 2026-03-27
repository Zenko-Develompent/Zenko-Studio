package com.hackathon.edu.service;

import com.hackathon.edu.dto.achievement.AchievementDTO;
import com.hackathon.edu.repository.AchievementUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AchievementQueryService {
    private final AchievementCatalogService achievementCatalogService;
    private final AchievementUserRepository achievementUserRepository;
    private final AchievementProgressService achievementProgressService;

    @Transactional
    public AchievementDTO.AchievementListResponse list(UUID userId) {
        Set<String> unlockedCodes;
        if (userId != null) {
            achievementProgressService.evaluateForUser(userId);
            unlockedCodes = new HashSet<>(achievementUserRepository.findAchievementCodesByUserId(userId));
        } else {
            unlockedCodes = Set.of();
        }

        final Set<String> finalUnlockedCodes = unlockedCodes;
        var items = achievementCatalogService.list().stream()
                .map(def -> new AchievementDTO.AchievementItem(
                        def.code(),
                        def.name(),
                        def.icon(),
                        def.order(),
                        finalUnlockedCodes.contains(def.code())
                ))
                .toList();

        return new AchievementDTO.AchievementListResponse(items);
    }
}
