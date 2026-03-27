package com.hackathon.edu.seed;

import com.hackathon.edu.entity.AchievementEntity;
import com.hackathon.edu.repository.AchievementRepository;
import com.hackathon.edu.service.AchievementCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AchievementCatalogSyncRunner implements CommandLineRunner {
    private final AchievementCatalogService achievementCatalogService;
    private final AchievementRepository achievementRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (AchievementCatalogService.AchievementDefinition definition : achievementCatalogService.list()) {
            AchievementEntity entity = achievementRepository.findByCode(definition.code())
                    .orElseGet(AchievementEntity::new);
            entity.setCode(definition.code());
            entity.setName(definition.name());
            entity.setDescription(definition.description());
            entity.setIcon(definition.icon());
            achievementRepository.save(entity);
        }
    }
}
