package com.hackathon.edu.controller;

import com.hackathon.edu.dto.social.SocialChatDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.SocialChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/social/chats")
@RequiredArgsConstructor
public class SocialChatController {
    private final SocialChatService socialChatService;
    private final AuthService authService;

    @PostMapping("/private/{otherUserId}")
    public SocialChatDTO.CreatePrivateChatResponse createPrivateChat(
            @PathVariable("otherUserId") UUID otherUserId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialChatService.createPrivateChat(userId, otherUserId);
    }

    @GetMapping
    public SocialChatDTO.ChatListResponse chats(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialChatService.listChats(userId, limit);
    }

    @GetMapping("/{chatId}/messages")
    public SocialChatDTO.MessagesResponse messages(
            @PathVariable("chatId") UUID chatId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestParam(name = "beforeMessageId", required = false) Long beforeMessageId,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialChatService.getMessages(userId, chatId, beforeMessageId, limit);
    }

    @PostMapping("/{chatId}/messages")
    public SocialChatDTO.SendMessageResponse sendMessage(
            @PathVariable("chatId") UUID chatId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody SocialChatDTO.SendMessageRequest request
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialChatService.sendMessage(userId, chatId, request);
    }

    @PutMapping("/{chatId}/messages/{messageId}")
    public SocialChatDTO.EditMessageResponse editMessage(
            @PathVariable("chatId") UUID chatId,
            @PathVariable("messageId") Long messageId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody SocialChatDTO.EditMessageRequest request
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialChatService.editMessage(userId, chatId, messageId, request);
    }

    @DeleteMapping("/{chatId}/messages/{messageId}")
    public SocialChatDTO.DeleteMessageResponse deleteMessage(
            @PathVariable("chatId") UUID chatId,
            @PathVariable("messageId") Long messageId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialChatService.deleteMessage(userId, chatId, messageId);
    }

    @PostMapping("/{chatId}/read")
    public SocialChatDTO.MarkReadResponse markRead(
            @PathVariable("chatId") UUID chatId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) SocialChatDTO.MarkReadRequest request
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        Long lastReadMessageId = request == null ? null : request.lastReadMessageId();
        return socialChatService.markRead(userId, chatId, lastReadMessageId);
    }
}
