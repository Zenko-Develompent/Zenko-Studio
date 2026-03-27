package com.hackathon.edu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AchievementCatalogService {
    private static final String CATALOG_PATH = "catalog/achievements.json";

    private final ObjectMapper objectMapper;

    private volatile List<AchievementDefinition> cachedList;
    private volatile Map<String, AchievementDefinition> cachedByCode;

    public AchievementCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<AchievementDefinition> list() {
        ensureLoaded();
        return cachedList;
    }

    public AchievementDefinition requireByCode(String code) {
        ensureLoaded();
        AchievementDefinition definition = cachedByCode.get(code);
        if (definition == null) {
            throw new IllegalStateException("Achievement not found in catalog: " + code);
        }
        return definition;
    }

    private void ensureLoaded() {
        if (cachedList != null && cachedByCode != null) {
            return;
        }
        synchronized (this) {
            if (cachedList != null && cachedByCode != null) {
                return;
            }
            List<AchievementDefinition> loaded = loadFromJson();
            cachedList = loaded;
            cachedByCode = loaded.stream()
                    .collect(Collectors.toMap(AchievementDefinition::code, it -> it));
        }
    }

    private List<AchievementDefinition> loadFromJson() {
        try (InputStream in = new ClassPathResource(CATALOG_PATH).getInputStream()) {
            List<AchievementDefinition> raw = objectMapper.readValue(
                    in,
                    new TypeReference<List<AchievementDefinition>>() {
                    }
            );
            if (raw == null || raw.isEmpty()) {
                throw new IllegalStateException("Achievement catalog is empty");
            }
            return raw.stream()
                    .filter(it -> it != null && hasText(it.code()) && hasText(it.name()) && hasText(it.icon()))
                    .sorted(Comparator.comparingInt(AchievementDefinition::order))
                    .toList();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load achievement catalog from " + CATALOG_PATH, ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record AchievementDefinition(
            String code,
            String name,
            String icon,
            int order
    ) {
    }
}
