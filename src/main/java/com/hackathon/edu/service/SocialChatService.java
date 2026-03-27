package com.hackathon.edu.service;

import com.hackathon.edu.config.AppChatProperties;
import com.hackathon.edu.dto.social.SocialChatDTO;
import com.hackathon.edu.entity.ChatMessageEntity;
import com.hackathon.edu.entity.ChatReadEntity;
import com.hackathon.edu.entity.PrivateChatEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.ChatMessageRepository;
import com.hackathon.edu.repository.ChatReadRepository;
import com.hackathon.edu.repository.PrivateChatRepository;
import com.hackathon.edu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialChatService {
    private final PrivateChatRepository privateChatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReadRepository chatReadRepository;
    private final UserRepository userRepository;
    private final AppChatProperties appChatProperties;
    private final SocialRealtimeService socialRealtimeService;
    private final ActivityEventService activityEventService;
    private final AchievementProgressService achievementProgressService;

    @Transactional
    public SocialChatDTO.CreatePrivateChatResponse createPrivateChat(UUID userId, UUID otherUserId) {
        ensureNotParent(requireUser(userId));

        if (userId.equals(otherUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "chat_self");
        }
        ensureNotParent(requireUser(otherUserId));

        Pair pair = orderedPair(userId, otherUserId);
        Optional<PrivateChatEntity> existing = privateChatRepository.findByUserLowIdAndUserHighId(pair.low(), pair.high());
        if (existing.isPresent()) {
            return new SocialChatDTO.CreatePrivateChatResponse(existing.get().getChatId(), otherUserId, false);
        }

        PrivateChatEntity chat = new PrivateChatEntity();
        chat.setUserLowId(pair.low());
        chat.setUserHighId(pair.high());
        chat = privateChatRepository.save(chat);
        socialRealtimeService.publishChatCreated(chat, userId);
        return new SocialChatDTO.CreatePrivateChatResponse(chat.getChatId(), otherUserId, true);
    }

    public SocialChatDTO.ChatListResponse listChats(UUID userId, Integer limit) {
        ensureNotParent(requireUser(userId));

        int safeLimit = normalizeLimit(limit, appChatProperties.getChatsDefaultLimit(), appChatProperties.getChatsMaxLimit());
        List<PrivateChatEntity> chats = privateChatRepository.findForUser(userId, PageRequest.of(0, safeLimit));

        Set<UUID> otherIds = chats.stream()
                .map(chat -> resolveOtherUserId(chat, userId))
                .collect(java.util.stream.Collectors.toSet());
        Map<UUID, UserEntity> users = getUsersMap(otherIds);

        List<SocialChatDTO.ChatItem> items = new ArrayList<>(chats.size());
        for (PrivateChatEntity chat : chats) {
            UUID otherUserId = resolveOtherUserId(chat, userId);
            UserEntity other = users.get(otherUserId);
            if (other != null && isParentRole(other)) {
                continue;
            }

            ChatMessageEntity lastMessage = chatMessageRepository.findTopByChat_ChatIdOrderByMessageIdDesc(chat.getChatId())
                    .orElse(null);
            Long lastReadId = chatReadRepository.findByChat_ChatIdAndUserId(chat.getChatId(), userId)
                    .map(ChatReadEntity::getLastReadMessageId)
                    .orElse(0L);
            long unread = lastMessage == null
                    ? 0L
                    : chatMessageRepository.countByChat_ChatIdAndSenderUserIdNotAndMessageIdGreaterThan(
                            chat.getChatId(),
                            userId,
                            lastReadId == null ? 0L : lastReadId
                    );

            items.add(new SocialChatDTO.ChatItem(
                    chat.getChatId(),
                    otherUserId,
                    other == null ? null : other.getUsername(),
                    toPreview(lastMessage),
                    unread,
                    chat.getUpdatedAt()
            ));
        }

        return new SocialChatDTO.ChatListResponse(items);
    }

    public SocialChatDTO.MessagesResponse getMessages(UUID userId, UUID chatId, Long beforeMessageId, Integer limit) {
        PrivateChatEntity chat = requireAccessibleChat(userId, chatId);

        int safeLimit = normalizeLimit(
                limit,
                appChatProperties.getMessagesDefaultLimit(),
                appChatProperties.getMessagesMaxLimit()
        );
        List<ChatMessageEntity> desc = chatMessageRepository.findPage(chat.getChatId(), beforeMessageId, PageRequest.of(0, safeLimit));
        List<ChatMessageEntity> asc = new ArrayList<>(desc);
        java.util.Collections.reverse(asc);

        List<SocialChatDTO.MessageItem> items = asc.stream()
                .map(this::toMessageItem)
                .toList();

        return new SocialChatDTO.MessagesResponse(items);
    }

    @Transactional
    public SocialChatDTO.SendMessageResponse sendMessage(UUID userId, UUID chatId, SocialChatDTO.SendMessageRequest request) {
        PrivateChatEntity chat = requireAccessibleChat(userId, chatId);

        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "message_empty");
        }

        Long replyToMessageId = request.replyToMessageId();
        if (replyToMessageId != null) {
            ChatMessageEntity reply = chatMessageRepository.findById(replyToMessageId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "reply_message_not_found"));
            if (reply.getChat() == null || !chatId.equals(reply.getChat().getChatId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "reply_message_not_found");
            }
        }

        ChatMessageEntity message = new ChatMessageEntity();
        message.setChat(chat);
        message.setSenderUserId(userId);
        message.setText(text);
        message.setReplyToMessageId(replyToMessageId);
        message = chatMessageRepository.save(message);

        chat.setLastMessageAt(message.getCreatedAt());
        privateChatRepository.save(chat);

        SocialChatDTO.MessageItem messageItem = toMessageItem(message);
        socialRealtimeService.publishMessageSent(chat, messageItem);
        activityEventService.recordMessageSent(userId, chat.getChatId(), message.getMessageId());
        achievementProgressService.evaluateForUser(userId);
        return new SocialChatDTO.SendMessageResponse(messageItem);
    }

    @Transactional
    public SocialChatDTO.EditMessageResponse editMessage(
            UUID userId,
            UUID chatId,
            Long messageId,
            SocialChatDTO.EditMessageRequest request
    ) {
        requireAccessibleChat(userId, chatId);
        ChatMessageEntity message = requireOwnMessage(userId, chatId, messageId);

        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "message_empty");
        }

        message.setText(text);
        message = chatMessageRepository.save(message);
        SocialChatDTO.MessageItem messageItem = toMessageItem(message);
        socialRealtimeService.publishMessageEdited(message.getChat(), messageItem);
        return new SocialChatDTO.EditMessageResponse(messageItem);
    }

    @Transactional
    public SocialChatDTO.DeleteMessageResponse deleteMessage(UUID userId, UUID chatId, Long messageId) {
        PrivateChatEntity chat = requireAccessibleChat(userId, chatId);
        ChatMessageEntity message = requireOwnMessage(userId, chatId, messageId);

        chatMessageRepository.delete(message);

        ChatMessageEntity last = chatMessageRepository.findTopByChat_ChatIdOrderByMessageIdDesc(chatId).orElse(null);
        chat.setLastMessageAt(last == null ? null : last.getCreatedAt());
        privateChatRepository.save(chat);

        socialRealtimeService.publishMessageDeleted(chat, messageId, userId);
        return new SocialChatDTO.DeleteMessageResponse(true, messageId);
    }

    @Transactional
    public SocialChatDTO.MarkReadResponse markRead(UUID userId, UUID chatId, Long requestedLastReadMessageId) {
        PrivateChatEntity chat = requireAccessibleChat(userId, chatId);

        long lastReadMessageId = requestedLastReadMessageId == null ? 0L : requestedLastReadMessageId;
        if (lastReadMessageId < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "last_read_message_invalid");
        }
        if (lastReadMessageId > 0) {
            ChatMessageEntity message = chatMessageRepository.findById(lastReadMessageId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "message_not_found"));
            if (message.getChat() == null || !chatId.equals(message.getChat().getChatId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "message_not_found");
            }
        }

        ChatReadEntity row = chatReadRepository.findByChat_ChatIdAndUserId(chatId, userId)
                .orElseGet(() -> createReadRow(chat, userId));

        long next = Math.max(row.getLastReadMessageId() == null ? 0L : row.getLastReadMessageId(), lastReadMessageId);
        row.setLastReadMessageId(next);
        row.setLastReadAt(OffsetDateTime.now());
        chatReadRepository.save(row);

        socialRealtimeService.publishMessageRead(chat, next, userId);
        return new SocialChatDTO.MarkReadResponse(true, next);
    }

    private ChatReadEntity createReadRow(PrivateChatEntity chat, UUID userId) {
        ChatReadEntity row = new ChatReadEntity();
        row.setChat(chat);
        row.setUserId(userId);
        row.setLastReadMessageId(0L);
        row.setLastReadAt(OffsetDateTime.now());
        return chatReadRepository.save(row);
    }

    private SocialChatDTO.MessagePreview toPreview(ChatMessageEntity message) {
        if (message == null) {
            return null;
        }
        return new SocialChatDTO.MessagePreview(
                message.getMessageId(),
                message.getSenderUserId(),
                message.getText(),
                message.getCreatedAt()
        );
    }

    private SocialChatDTO.MessageItem toMessageItem(ChatMessageEntity message) {
        return new SocialChatDTO.MessageItem(
                message.getMessageId(),
                message.getChat() == null ? null : message.getChat().getChatId(),
                message.getSenderUserId(),
                message.getText(),
                message.getReplyToMessageId(),
                message.getCreatedAt()
        );
    }

    private PrivateChatEntity requireAccessibleChat(UUID userId, UUID chatId) {
        PrivateChatEntity chat = privateChatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "chat_not_found"));

        if (!isParticipant(chat, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "chat_access_denied");
        }
        ensureNotParent(requireUser(userId));
        ensureChatNotForParent(chat);
        return chat;
    }

    private ChatMessageEntity requireOwnMessage(UUID userId, UUID chatId, Long messageId) {
        ChatMessageEntity message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "message_not_found"));

        if (message.getChat() == null || !chatId.equals(message.getChat().getChatId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "message_not_found");
        }
        if (!userId.equals(message.getSenderUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden");
        }
        return message;
    }

    private boolean isParticipant(PrivateChatEntity chat, UUID userId) {
        return userId.equals(chat.getUserLowId()) || userId.equals(chat.getUserHighId());
    }

    private UUID resolveOtherUserId(PrivateChatEntity chat, UUID currentUserId) {
        return currentUserId.equals(chat.getUserLowId()) ? chat.getUserHighId() : chat.getUserLowId();
    }

    private Pair orderedPair(UUID first, UUID second) {
        return first.compareTo(second) <= 0 ? new Pair(first, second) : new Pair(second, first);
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found"));
    }

    private Map<UUID, UserEntity> getUsersMap(Set<UUID> userIds) {
        Map<UUID, UserEntity> map = new HashMap<>();
        for (UserEntity user : userRepository.findAllById(userIds)) {
            map.put(user.getUserId(), user);
        }
        return map;
    }

    private int normalizeLimit(Integer value, int defaultValue, int maxValue) {
        int safe = value == null || value <= 0 ? defaultValue : value;
        return Math.min(safe, maxValue);
    }

    private void ensureNotParent(UserEntity user) {
        if (isParentRole(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "social_not_available_for_parent");
        }
    }

    private void ensureChatNotForParent(PrivateChatEntity chat) {
        UserEntity low = requireUser(chat.getUserLowId());
        UserEntity high = requireUser(chat.getUserHighId());
        if (isParentRole(low) || isParentRole(high)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "chat_with_parent_forbidden");
        }
    }

    private boolean isParentRole(UserEntity user) {
        if (user == null || user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }
        return "parent".equals(user.getRole().getName().trim().toLowerCase(Locale.ROOT));
    }

    private record Pair(UUID low, UUID high) {
    }
}
