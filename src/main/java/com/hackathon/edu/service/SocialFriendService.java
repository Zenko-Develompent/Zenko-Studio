package com.hackathon.edu.service;

import com.hackathon.edu.config.AppChatProperties;
import com.hackathon.edu.dto.social.SocialFriendDTO;
import com.hackathon.edu.entity.FriendRequestEntity;
import com.hackathon.edu.entity.FriendshipEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.FriendRequestRepository;
import com.hackathon.edu.repository.FriendshipRepository;
import com.hackathon.edu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialFriendService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final AppChatProperties appChatProperties;
    private final SocialRealtimeService socialRealtimeService;
    private final AchievementProgressService achievementProgressService;

    @Transactional
    public SocialFriendDTO.SendRequestResponse sendRequest(UUID requesterUserId, UUID receiverUserId) {
        UserEntity requester = requireUser(requesterUserId);
        ensureNotParent(requester);

        if (requesterUserId.equals(receiverUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "friend_request_self");
        }
        UserEntity receiver = requireUser(receiverUserId);
        ensureNotParent(receiver);

        if (friendshipRepository.existsByUserIdAndFriendUserId(requesterUserId, receiverUserId)) {
            throw new ApiException(HttpStatus.CONFLICT, "friendship_already_exists");
        }
        if (friendRequestRepository.existsByRequesterUserIdAndReceiverUserIdAndStatus(
                requesterUserId,
                receiverUserId,
                STATUS_PENDING
        )) {
            throw new ApiException(HttpStatus.CONFLICT, "friend_request_already_exists");
        }
        if (friendRequestRepository.existsByRequesterUserIdAndReceiverUserIdAndStatus(
                receiverUserId,
                requesterUserId,
                STATUS_PENDING
        )) {
            throw new ApiException(HttpStatus.CONFLICT, "friend_request_incoming_exists");
        }

        FriendRequestEntity request = new FriendRequestEntity();
        request.setRequesterUserId(requesterUserId);
        request.setReceiverUserId(receiverUserId);
        request.setStatus(STATUS_PENDING);
        request = friendRequestRepository.save(request);
        socialRealtimeService.publishFriendRequest(request);

        return new SocialFriendDTO.SendRequestResponse(request.getRequestId(), request.getStatus());
    }

    public SocialFriendDTO.RequestsResponse incoming(UUID userId, Integer limit) {
        ensureNotParent(requireUser(userId));

        int safeLimit = normalizeLimit(
                limit,
                appChatProperties.getFriendRequestsDefaultLimit(),
                appChatProperties.getFriendRequestsMaxLimit()
        );
        List<FriendRequestEntity> rows = friendRequestRepository.findByReceiverUserIdAndStatusOrderByCreatedAtDesc(userId, STATUS_PENDING)
                .stream()
                .limit(safeLimit)
                .toList();
        return new SocialFriendDTO.RequestsResponse(toRequestItems(rows));
    }

    public SocialFriendDTO.RequestsResponse outgoing(UUID userId, Integer limit) {
        ensureNotParent(requireUser(userId));

        int safeLimit = normalizeLimit(
                limit,
                appChatProperties.getFriendRequestsDefaultLimit(),
                appChatProperties.getFriendRequestsMaxLimit()
        );
        List<FriendRequestEntity> rows = friendRequestRepository.findByRequesterUserIdAndStatusOrderByCreatedAtDesc(userId, STATUS_PENDING)
                .stream()
                .limit(safeLimit)
                .toList();
        return new SocialFriendDTO.RequestsResponse(toRequestItems(rows));
    }

    @Transactional
    public SocialFriendDTO.AcceptRejectResponse accept(UUID userId, UUID requestId) {
        ensureNotParent(requireUser(userId));

        FriendRequestEntity request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "friend_request_not_found"));

        if (!userId.equals(request.getReceiverUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "friend_request_not_pending");
        }
        ensureNotParent(requireUser(request.getRequesterUserId()));
        ensureNotParent(requireUser(request.getReceiverUserId()));

        request.setStatus(STATUS_ACCEPTED);
        request.setRespondedAt(OffsetDateTime.now());
        friendRequestRepository.save(request);

        ensureFriendship(request.getRequesterUserId(), request.getReceiverUserId());
        ensureFriendship(request.getReceiverUserId(), request.getRequesterUserId());
        socialRealtimeService.publishFriendAccepted(request);
        achievementProgressService.evaluateForUser(request.getRequesterUserId());
        achievementProgressService.evaluateForUser(request.getReceiverUserId());

        return new SocialFriendDTO.AcceptRejectResponse(request.getRequestId(), request.getStatus());
    }

    @Transactional
    public SocialFriendDTO.AcceptRejectResponse reject(UUID userId, UUID requestId) {
        ensureNotParent(requireUser(userId));

        FriendRequestEntity request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "friend_request_not_found"));

        if (!userId.equals(request.getReceiverUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "friend_request_not_pending");
        }
        ensureNotParent(requireUser(request.getRequesterUserId()));
        ensureNotParent(requireUser(request.getReceiverUserId()));

        request.setStatus(STATUS_REJECTED);
        request.setRespondedAt(OffsetDateTime.now());
        friendRequestRepository.save(request);
        socialRealtimeService.publishFriendRejected(request);

        return new SocialFriendDTO.AcceptRejectResponse(request.getRequestId(), request.getStatus());
    }

    public SocialFriendDTO.FriendsResponse friends(UUID userId, Integer limit) {
        ensureNotParent(requireUser(userId));

        int safeLimit = normalizeLimit(
                limit,
                appChatProperties.getFriendsDefaultLimit(),
                appChatProperties.getFriendsMaxLimit()
        );
        List<FriendshipEntity> rows = friendshipRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(safeLimit)
                .toList();

        Set<UUID> friendIds = new HashSet<>();
        for (FriendshipEntity row : rows) {
            friendIds.add(row.getFriendUserId());
        }
        Map<UUID, UserEntity> users = getUsersMap(friendIds);

        List<SocialFriendDTO.FriendItem> items = rows.stream()
                .map(row -> new SocialFriendDTO.FriendItem(
                        row.getFriendUserId(),
                        users.get(row.getFriendUserId()) == null ? null : users.get(row.getFriendUserId()).getUsername(),
                        row.getCreatedAt()
                ))
                .toList();
        return new SocialFriendDTO.FriendsResponse(items);
    }

    @Transactional
    public void removeFriend(UUID userId, UUID friendUserId) {
        ensureNotParent(requireUser(userId));
        ensureNotParent(requireUser(friendUserId));

        boolean existed = friendshipRepository.existsByUserIdAndFriendUserId(userId, friendUserId)
                || friendshipRepository.existsByUserIdAndFriendUserId(friendUserId, userId);
        friendshipRepository.deleteByUserIdAndFriendUserId(userId, friendUserId);
        friendshipRepository.deleteByUserIdAndFriendUserId(friendUserId, userId);
        if (existed) {
            socialRealtimeService.publishFriendRemoved(userId, friendUserId);
            achievementProgressService.evaluateForUser(userId);
            achievementProgressService.evaluateForUser(friendUserId);
        }
    }

    public SocialFriendDTO.UserSearchResponse searchUsers(UUID currentUserId, String queryRaw, Integer limit) {
        ensureNotParent(requireUser(currentUserId));

        int safeLimit = normalizeLimit(
                limit,
                appChatProperties.getUserSearchDefaultLimit(),
                appChatProperties.getUserSearchMaxLimit()
        );
        String query = queryRaw == null ? "" : queryRaw.trim();

        if (query.length() < appChatProperties.getUserSearchMinQueryLength()) {
            return new SocialFriendDTO.UserSearchResponse(List.of());
        }
        if (query.length() > appChatProperties.getUserSearchMaxQueryLength()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "search_query_too_long");
        }

        List<UserEntity> users = userRepository.searchByUsername(currentUserId, query, PageRequest.of(0, safeLimit));
        List<SocialFriendDTO.UserSearchItem> items = users.stream()
                .filter(user -> !isParentRole(user))
                .map(user -> new SocialFriendDTO.UserSearchItem(
                        user.getUserId(),
                        user.getUsername(),
                        resolveFriendStatus(currentUserId, user.getUserId())
                ))
                .toList();
        return new SocialFriendDTO.UserSearchResponse(items);
    }

    private String resolveFriendStatus(UUID userId, UUID otherUserId) {
        if (isParentRole(requireUser(otherUserId))) {
            return null;
        }
        if (friendshipRepository.existsByUserIdAndFriendUserId(userId, otherUserId)) {
            return "ACCEPTED";
        }
        if (friendRequestRepository.existsByRequesterUserIdAndReceiverUserIdAndStatus(
                userId,
                otherUserId,
                STATUS_PENDING
        )) {
            return "OUTGOING_REQUEST";
        }
        if (friendRequestRepository.existsByRequesterUserIdAndReceiverUserIdAndStatus(
                otherUserId,
                userId,
                STATUS_PENDING
        )) {
            return "INCOMING_REQUEST";
        }
        return null;
    }

    private List<SocialFriendDTO.RequestItem> toRequestItems(List<FriendRequestEntity> rows) {
        Set<UUID> ids = new HashSet<>();
        for (FriendRequestEntity row : rows) {
            ids.add(row.getRequesterUserId());
            ids.add(row.getReceiverUserId());
        }
        Map<UUID, UserEntity> users = getUsersMap(ids);

        return rows.stream()
                .map(row -> new SocialFriendDTO.RequestItem(
                        row.getRequestId(),
                        row.getRequesterUserId(),
                        users.get(row.getRequesterUserId()) == null ? null : users.get(row.getRequesterUserId()).getUsername(),
                        row.getReceiverUserId(),
                        users.get(row.getReceiverUserId()) == null ? null : users.get(row.getReceiverUserId()).getUsername(),
                        row.getStatus(),
                        row.getCreatedAt(),
                        row.getRespondedAt()
                ))
                .toList();
    }

    private Map<UUID, UserEntity> getUsersMap(Set<UUID> userIds) {
        Map<UUID, UserEntity> map = new HashMap<>();
        for (UserEntity user : userRepository.findAllById(userIds)) {
            if (isParentRole(user)) {
                continue;
            }
            map.put(user.getUserId(), user);
        }
        return map;
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found"));
    }

    private void ensureFriendship(UUID userId, UUID friendUserId) {
        if (friendshipRepository.existsByUserIdAndFriendUserId(userId, friendUserId)) {
            return;
        }
        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setUserId(userId);
        friendship.setFriendUserId(friendUserId);
        try {
            friendshipRepository.save(friendship);
        } catch (DataIntegrityViolationException ignore) {
        }
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

    private boolean isParentRole(UserEntity user) {
        if (user == null || user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }
        return "parent".equals(user.getRole().getName().trim().toLowerCase(Locale.ROOT));
    }
}
