package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ChatMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    Optional<ChatMessageEntity> findTopByChat_ChatIdOrderByMessageIdDesc(UUID chatId);

    @Query("""
            select m from ChatMessageEntity m
            where m.chat.chatId = :chatId and (:beforeMessageId is null or m.messageId < :beforeMessageId)
            order by m.messageId desc
            """)
    List<ChatMessageEntity> findPage(
            @Param("chatId") UUID chatId,
            @Param("beforeMessageId") Long beforeMessageId,
            Pageable pageable
    );

    long countByChat_ChatIdAndSenderUserIdNotAndMessageIdGreaterThan(UUID chatId, UUID senderUserId, Long messageId);

    long countBySenderUserId(UUID senderUserId);
}
