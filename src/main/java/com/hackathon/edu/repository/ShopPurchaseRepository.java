package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ShopPurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShopPurchaseRepository extends JpaRepository<ShopPurchaseEntity, UUID> {
    long countByUserId(UUID userId);

    long countByUserIdAndCountsAsUpgradeTrue(UUID userId);

    boolean existsByUserIdAndItemId(UUID userId, String itemId);

    @Query("""
            select coalesce(sum(p.itemPrice), 0)
            from ShopPurchaseEntity p
            where p.userId = :userId
            """)
    long sumSpentByUserId(@Param("userId") UUID userId);

    @Query("""
            select distinct p.itemId
            from ShopPurchaseEntity p
            where p.userId = :userId
            """)
    List<String> findDistinctItemIdsByUserId(@Param("userId") UUID userId);
}
