package com.hackathon.edu.controller;

import com.hackathon.edu.dto.social.SocialFriendDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.SocialFriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialFriendController {
    private final SocialFriendService socialFriendService;
    private final AuthService authService;

    @PostMapping("/friends/requests")
    public SocialFriendDTO.SendRequestResponse sendRequest(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody SocialFriendDTO.SendRequestRequest request
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialFriendService.sendRequest(userId, request.userId());
    }

    @GetMapping("/friends/requests/incoming")
    public SocialFriendDTO.RequestsResponse incoming(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialFriendService.incoming(userId, limit);
    }

    @GetMapping("/friends/requests/outgoing")
    public SocialFriendDTO.RequestsResponse outgoing(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialFriendService.outgoing(userId, limit);
    }

    @PostMapping("/friends/requests/{requestId}/accept")
    public SocialFriendDTO.AcceptRejectResponse accept(
            @PathVariable("requestId") UUID requestId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialFriendService.accept(userId, requestId);
    }

    @PostMapping("/friends/requests/{requestId}/reject")
    public SocialFriendDTO.AcceptRejectResponse reject(
            @PathVariable("requestId") UUID requestId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialFriendService.reject(userId, requestId);
    }

    @GetMapping("/friends")
    public SocialFriendDTO.FriendsResponse friends(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialFriendService.friends(userId, limit);
    }

    @DeleteMapping("/friends/{friendUserId}")
    public void removeFriend(
            @PathVariable("friendUserId") UUID friendUserId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        socialFriendService.removeFriend(userId, friendUserId);
    }

    @GetMapping("/users/search")
    public SocialFriendDTO.UserSearchResponse searchUsers(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return socialFriendService.searchUsers(userId, q, limit);
    }
}
