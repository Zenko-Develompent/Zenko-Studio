package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ChatReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatReadRepository extends JpaRepository<ChatReadEntity, UUID> {
    Optional<ChatReadEntity> findByChat_ChatIdAndUserId(UUID chatId, UUID userId);
}
