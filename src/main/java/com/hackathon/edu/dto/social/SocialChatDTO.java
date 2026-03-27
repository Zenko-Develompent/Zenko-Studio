package com.hackathon.edu.dto.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SocialChatDTO {
    private SocialChatDTO() {
    }

    public record CreatePrivateChatResponse(
            UUID chatId,
            UUID otherUserId,
            boolean created
    ) {
    }

    public record MessagePreview(
            Long messageId,
            UUID senderUserId,
            String text,
            OffsetDateTime createdAt
    ) {
    }

    public record ChatItem(
            UUID chatId,
            UUID otherUserId,
            String otherUsername,
            MessagePreview lastMessage,
            long unreadCount,
            OffsetDateTime updatedAt
    ) {
    }

    public record ChatListResponse(
            List<ChatItem> items
    ) {
    }

    public record MessageItem(
            Long messageId,
            UUID chatId,
            UUID senderUserId,
            String text,
            Long replyToMessageId,
            OffsetDateTime createdAt
    ) {
    }

    public record MessagesResponse(
            List<MessageItem> items
    ) {
    }

    public record SendMessageRequest(
            @NotBlank
            @Size(max = 2000)
            String text,
            Long replyToMessageId
    ) {
    }

    public record SendMessageResponse(
            MessageItem message
    ) {
    }

    public record EditMessageRequest(
            @NotBlank
            @Size(max = 2000)
            String text
    ) {
    }

    public record EditMessageResponse(
            MessageItem message
    ) {
    }

    public record DeleteMessageResponse(
            boolean deleted,
            Long messageId
    ) {
    }

    public record MarkReadRequest(
            Long lastReadMessageId
    ) {
    }

    public record MarkReadResponse(
            boolean ok,
            Long lastReadMessageId
    ) {
    }
}
