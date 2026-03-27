package com.hackathon.edu.dto.social;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SocialFriendDTO {
    private SocialFriendDTO() {
    }

    public record SendRequestRequest(
            @NotNull
            UUID userId
    ) {
    }

    public record RequestItem(
            UUID requestId,
            UUID requesterUserId,
            String requesterUsername,
            UUID receiverUserId,
            String receiverUsername,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime respondedAt
    ) {
    }

    public record RequestsResponse(
            List<RequestItem> items
    ) {
    }

    public record SendRequestResponse(
            UUID requestId,
            String status
    ) {
    }

    public record AcceptRejectResponse(
            UUID requestId,
            String status
    ) {
    }

    public record FriendItem(
            UUID userId,
            String username,
            OffsetDateTime since
    ) {
    }

    public record FriendsResponse(
            List<FriendItem> items
    ) {
    }

    public record UserSearchItem(
            UUID userId,
            String username,
            String friendStatus
    ) {
    }

    public record UserSearchResponse(
            List<UserSearchItem> users
    ) {
    }
}
