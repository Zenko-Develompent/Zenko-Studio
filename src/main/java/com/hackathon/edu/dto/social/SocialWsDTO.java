package com.hackathon.edu.dto.social;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class SocialWsDTO {
    private SocialWsDTO() {
    }

    public record EventEnvelope<T>(
            String type,
            OffsetDateTime timestamp,
            T data
    ) {
    }

    public record ChatCreatedData(
            UUID chatId,
            UUID otherUserId,
            UUID initiatorUserId
    ) {
    }

    public record ChatMessageData(
            UUID chatId,
            SocialChatDTO.MessageItem message
    ) {
    }

    public record ChatMessageDeletedData(
            UUID chatId,
            Long messageId,
            UUID actorUserId
    ) {
    }

    public record ChatReadData(
            UUID chatId,
            Long lastReadMessageId,
            UUID readerUserId
    ) {
    }

    public record FriendRequestData(
            UUID requestId,
            UUID requesterUserId,
            UUID receiverUserId,
            String status
    ) {
    }

    public record FriendRemovedData(
            UUID friendUserId
    ) {
    }

    public record ParentControlRequestData(
            UUID requestId,
            UUID parentUserId,
            UUID childUserId,
            String status
    ) {
    }
}
