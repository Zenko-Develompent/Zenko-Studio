package com.hackathon.edu.service;

import com.hackathon.edu.config.AppChatProperties;
import com.hackathon.edu.dto.social.SocialChatDTO;
import com.hackathon.edu.dto.social.SocialWsDTO;
import com.hackathon.edu.entity.FriendRequestEntity;
import com.hackathon.edu.entity.ParentControlRequestEntity;
import com.hackathon.edu.entity.PrivateChatEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialRealtimeService {
    private final SimpMessagingTemplate messagingTemplate;
    private final AppChatProperties appChatProperties;

    public void publishChatCreated(PrivateChatEntity chat, UUID initiatorUserId) {
        if (chat == null || chat.getChatId() == null || chat.getUserLowId() == null || chat.getUserHighId() == null) {
            return;
        }
        sendChatEvent(
                chat.getUserLowId(),
                "chat_created",
                new SocialWsDTO.ChatCreatedData(chat.getChatId(), chat.getUserHighId(), initiatorUserId)
        );
        sendChatEvent(
                chat.getUserHighId(),
                "chat_created",
                new SocialWsDTO.ChatCreatedData(chat.getChatId(), chat.getUserLowId(), initiatorUserId)
        );
    }

    public void publishMessageSent(PrivateChatEntity chat, SocialChatDTO.MessageItem message) {
        publishMessageEvent(chat, "message_sent", message);
    }

    public void publishMessageEdited(PrivateChatEntity chat, SocialChatDTO.MessageItem message) {
        publishMessageEvent(chat, "message_edited", message);
    }

    public void publishMessageDeleted(PrivateChatEntity chat, Long messageId, UUID actorUserId) {
        if (chat == null || messageId == null) {
            return;
        }
        SocialWsDTO.ChatMessageDeletedData data = new SocialWsDTO.ChatMessageDeletedData(
                chat.getChatId(),
                messageId,
                actorUserId
        );
        sendToParticipants(chat, "message_deleted", data);
    }

    public void publishMessageRead(PrivateChatEntity chat, Long lastReadMessageId, UUID readerUserId) {
        if (chat == null) {
            return;
        }
        SocialWsDTO.ChatReadData data = new SocialWsDTO.ChatReadData(chat.getChatId(), lastReadMessageId, readerUserId);
        sendToParticipants(chat, "message_read", data);
    }

    public void publishFriendRequest(FriendRequestEntity request) {
        publishFriendRequestEvent(request, "friend_request");
    }

    public void publishFriendAccepted(FriendRequestEntity request) {
        publishFriendRequestEvent(request, "friend_accept");
    }

    public void publishFriendRejected(FriendRequestEntity request) {
        publishFriendRequestEvent(request, "friend_reject");
    }

    public void publishFriendRemoved(UUID userId, UUID friendUserId) {
        if (userId == null || friendUserId == null) {
            return;
        }
        sendFriendEvent(userId, "friend_removed", new SocialWsDTO.FriendRemovedData(friendUserId));
        sendFriendEvent(friendUserId, "friend_removed", new SocialWsDTO.FriendRemovedData(userId));
    }

    public void publishParentControlRequest(ParentControlRequestEntity request) {
        publishParentControlEvent(request, "parent_control_request");
    }

    public void publishParentControlAccepted(ParentControlRequestEntity request) {
        publishParentControlEvent(request, "parent_control_accept");
    }

    public void publishParentControlRejected(ParentControlRequestEntity request) {
        publishParentControlEvent(request, "parent_control_reject");
    }

    private void publishMessageEvent(PrivateChatEntity chat, String type, SocialChatDTO.MessageItem message) {
        if (chat == null || message == null) {
            return;
        }
        SocialWsDTO.ChatMessageData data = new SocialWsDTO.ChatMessageData(chat.getChatId(), message);
        sendToParticipants(chat, type, data);
    }

    private void publishFriendRequestEvent(FriendRequestEntity request, String type) {
        if (request == null || request.getRequestId() == null) {
            return;
        }
        SocialWsDTO.FriendRequestData data = new SocialWsDTO.FriendRequestData(
                request.getRequestId(),
                request.getRequesterUserId(),
                request.getReceiverUserId(),
                request.getStatus()
        );
        sendFriendEvent(request.getRequesterUserId(), type, data);
        sendFriendEvent(request.getReceiverUserId(), type, data);
    }

    private void publishParentControlEvent(ParentControlRequestEntity request, String type) {
        if (request == null || request.getRequestId() == null) {
            return;
        }
        SocialWsDTO.ParentControlRequestData data = new SocialWsDTO.ParentControlRequestData(
                request.getRequestId(),
                request.getParentUserId(),
                request.getChildUserId(),
                request.getStatus()
        );
        sendParentControlEvent(request.getParentUserId(), type, data);
        sendParentControlEvent(request.getChildUserId(), type, data);
    }

    private void sendToParticipants(PrivateChatEntity chat, String type, Object data) {
        if (chat.getUserLowId() == null || chat.getUserHighId() == null) {
            return;
        }
        sendChatEvent(chat.getUserLowId(), type, data);
        sendChatEvent(chat.getUserHighId(), type, data);
    }

    private void sendChatEvent(UUID userId, String type, Object data) {
        sendUserEvent(userId, appChatProperties.getUserChatDestination(), type, data);
    }

    private void sendFriendEvent(UUID userId, String type, Object data) {
        sendUserEvent(userId, appChatProperties.getUserFriendDestination(), type, data);
    }

    private void sendParentControlEvent(UUID userId, String type, Object data) {
        sendUserEvent(userId, appChatProperties.getUserParentControlDestination(), type, data);
    }

    private void sendUserEvent(UUID userId, String destination, String type, Object data) {
        if (userId == null || destination == null || destination.isBlank()) {
            return;
        }
        SocialWsDTO.EventEnvelope<Object> event = new SocialWsDTO.EventEnvelope<>(
                type,
                OffsetDateTime.now(),
                data
        );
        messagingTemplate.convertAndSendToUser(userId.toString(), destination, event);
    }
}
