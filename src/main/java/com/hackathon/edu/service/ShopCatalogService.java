package com.hackathon.edu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShopCatalogService {
    private static final String CATALOG_PATH = "catalog/shop-items.json";

    private final ObjectMapper objectMapper;

    private volatile List<ShopItemDefinition> cachedList;
    private volatile Map<String, ShopItemDefinition> cachedById;

    public ShopCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ShopItemDefinition> list() {
        ensureLoaded();
        return cachedList;
    }

    public ShopItemDefinition requireById(String itemId) {
        ensureLoaded();
        ShopItemDefinition item = cachedById.get(itemId);
        if (item == null) {
            throw new IllegalStateException("Shop item not found in catalog: " + itemId);
        }
        return item;
    }

    private void ensureLoaded() {
        if (cachedList != null && cachedById != null) {
            return;
        }
        synchronized (this) {
            if (cachedList != null && cachedById != null) {
                return;
            }
            List<ShopItemDefinition> loaded = loadFromJson();
            cachedList = loaded;
            cachedById = loaded.stream().collect(Collectors.toMap(ShopItemDefinition::id, it -> it));
        }
    }

    private List<ShopItemDefinition> loadFromJson() {
        try (InputStream in = new ClassPathResource(CATALOG_PATH).getInputStream()) {
            List<ShopItemDefinition> raw = objectMapper.readValue(
                    in,
                    new TypeReference<List<ShopItemDefinition>>() {
                    }
            );
            if (raw == null || raw.isEmpty()) {
                throw new IllegalStateException("Shop catalog is empty");
            }
            return raw.stream()
                    .filter(item -> item != null
                            && hasText(item.id())
                            && hasText(item.name())
                            && hasText(item.icon())
                            && item.price() >= 0
                    )
                    .toList();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load shop catalog from " + CATALOG_PATH, ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ShopItemDefinition(
            String id,
            String name,
            String description,
            String icon,
            int price,
            String shopGroup,
            boolean countsAsUpgrade,
            int xpBoostPercent,
            int quizTaskCharges,
            int examChargeCost
    ) {
    }
}
