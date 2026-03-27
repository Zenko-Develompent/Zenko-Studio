package com.hackathon.edu.repository;

import com.hackathon.edu.entity.PrivateChatEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivateChatRepository extends JpaRepository<PrivateChatEntity, UUID> {
    Optional<PrivateChatEntity> findByUserLowIdAndUserHighId(UUID userLowId, UUID userHighId);

    @Query("""
            select c from PrivateChatEntity c
            where c.userLowId = :userId or c.userHighId = :userId
            order by c.updatedAt desc, c.chatId desc
            """)
    List<PrivateChatEntity> findForUser(@Param("userId") UUID userId, Pageable pageable);
}
