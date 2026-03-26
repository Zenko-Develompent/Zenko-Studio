package com.hackathon.edu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.edu.config.AppSecurityProperties;
import com.hackathon.edu.dto.AuthResponse;
import com.hackathon.edu.dto.AuthUserDto;
import com.hackathon.edu.dto.LoginRequest;
import com.hackathon.edu.dto.LogoutRequest;
import com.hackathon.edu.dto.RefreshRequest;
import com.hackathon.edu.dto.RegisterResponse;
import com.hackathon.edu.dto.WebSessionDto;
import com.hackathon.edu.entity.LocalCredentialEntity;
import com.hackathon.edu.entity.RefreshTokenEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.entity.WebSessionEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.LocalCredentialRepository;
import com.hackathon.edu.repository.RefreshTokenRepository;
import com.hackathon.edu.repository.UserRepository;
import com.hackathon.edu.repository.UserRepository;
import com.hackathon.edu.security.JwtService;
import com.hackathon.edu.security.PasswordHasher;
import com.hackathon.edu.util.EmailValidator;
import com.hackathon.edu.util.PasswordPolicy;
import com.hackathon.edu.util.ProfileUrlBuilder;
import com.hackathon.edu.util.RequestInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final UserEntity userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final WebSessionService webSessionService;
    private final JwtService jwtService;
    private final AppSecurityProperties props;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, long[]> loginRateLimit = new ConcurrentHashMap<>();

    @Transactional
    public RegisterResponse register(String usernameRaw, String password, BirthDate) {
        String username = usernameRaw == null ? null : usernameRaw.trim();

        if (username == null || !username.matches("[A-Za-z0-9_]{3,16}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_username");
        }
        if (!PasswordPolicy.accept(password)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "weak_password");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "conflict");
        }

        String uniqueUsername = ensureUniqueUsername(username);

        UserEntity user = new UserEntity();
        user.setUsername(uniqueUsername);
        user.getPassword();
        user.getBirthDate();
        user.getRole();
        user.getXp();
        
        user = userRepository.save(user);
        

    }

    @Transactional
    public LoginResult login(LoginRequest request, RequestInfo requestInfo) {
        String ip = requestInfo.ip();
        if (!loginAllowed(ip)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts");
        }

        String id = firstNonBlank(request.id(), request.email(), request.username());
        String password = request.password();
        if (id == null || password == null || password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "bad_request");
        }

        UUID userId;
        if (EmailValidator.isValid(id)) {
            userId = localCredentialRepository.findByEmailIgnoreCase(id)
                    .map(LocalCredentialEntity::getUserId)
                    .orElse(null);
        } else {
            userId = userRepository.findByUsernameIgnoreCase(id)
                    .map(UserEntity::getId)
                    .orElse(null);
        }

        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
        }

        LocalCredentialEntity cred = localCredentialRepository.findById(userId).orElse(null);
        if (cred == null || !PasswordHasher.verify(password.toCharArray(), cred.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
        }

        loginClear(ip);

        String deviceId = UUID.randomUUID().toString();
        WebSessionEntity session = webSessionService.createSession(userId, deviceId, requestInfo.userAgent(), requestInfo.ip());
        RefreshTokenService.TokenPair pair = refreshTokenService.issue(userId, deviceId, requestInfo.ip(), requestInfo.userAgent());

        AuthUserDto user = toUserDto(requireUser(userId), cred, userProfileRepository.findById(userId).orElse(null));
        return buildLoginResult(pair, session, deviceId, user);
    }

    @Transactional
    public LoginResult refresh(
            RefreshRequest request,
            String refreshCookie,
            String sessionHeader,
            String deviceHeader,
            String sessionCookie,
            String deviceCookie,
            RequestInfo requestInfo
    ) {
        String presented = firstNonBlank(request == null ? null : request.refreshToken(), refreshCookie);
        RefreshTokenService.ParsedRefreshToken parsed = refreshTokenService.parse(presented);

        RefreshTokenEntity row = refreshTokenRepository.findById(parsed.tokenId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid"));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (row.getRevokedAt() != null || row.getReplacedBy() != null || row.getExpiresAt().isBefore(now)) {
            refreshTokenService.revokeFamily(row.getFamilyId());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid");
        }
        if (!refreshTokenService.matchesHash(parsed.tokenId(), parsed.raw(), row.getTokenHash())) {
            refreshTokenService.revokeFamily(row.getFamilyId());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid");
        }

        UUID userId = row.getUserId();
        String deviceId = firstNonBlank(deviceHeader, deviceCookie, row.getDeviceId(), UUID.randomUUID().toString());
        WebSessionEntity session = resolveOrCreateSession(userId, sessionHeader, sessionCookie, deviceId, requestInfo);

        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(row, deviceId, requestInfo.ip(), requestInfo.userAgent());
        LocalCredentialEntity cred = localCredentialRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid"));
        AuthUserDto user = toUserDto(requireUser(userId), cred, userProfileRepository.findById(userId).orElse(null));
        return buildLoginResult(pair, session, deviceId, user);
    }

    @Transactional
    public void logout(
            LogoutRequest request,
            String refreshCookie,
            String sessionHeader,
            String sessionCookie
    ) {
        String presented = firstNonBlank(request == null ? null : request.refreshToken(), refreshCookie);
        if (presented != null && presented.contains(".")) {
            try {
                RefreshTokenService.ParsedRefreshToken parsed = refreshTokenService.parse(presented);
                refreshTokenRepository.findById(parsed.tokenId())
                        .ifPresent(row -> refreshTokenService.revokeFamily(row.getFamilyId()));
            } catch (ApiException ignore) {
            }
        }

        String sessionRaw = firstNonBlank(sessionHeader, sessionCookie);
        UUID sessionId = parseUuidOrNull(sessionRaw);
        if (sessionId != null) {
            webSessionService.deactivate(sessionId);
        }
    }

    public UUID requireUserIdFromAccessHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        return jwtService.verify(token).userId();
    }

    public List<WebSessionDto> listWebSessions(UUID userId, String currentSessionCookie) {
        UUID current = parseUuidOrNull(currentSessionCookie);
        return webSessionService.listActive(userId).stream()
                .map(s -> new WebSessionDto(
                        s.getSessionId().toString(),
                        s.getDeviceId(),
                        s.getUserAgent(),
                        s.getIpAddress(),
                        toIso(s.getCreatedAt()),
                        toIso(s.getLastActivityAt()),
                        toIso(s.getExpiresAt()),
                        current != null && current.equals(s.getSessionId())
                ))
                .toList();
    }

    @Transactional
    public void revokeWebSession(UUID userId, UUID sessionId) {
        webSessionService.deactivateForUser(sessionId, userId);
    }

    public long refreshMaxAgeSeconds() {
        return props.getRefreshTtlDays() * 86400L;
    }

    public long sessionMaxAgeSeconds() {
        return props.getSessionTtlSec();
    }

    private LoginResult buildLoginResult(RefreshTokenService.TokenPair pair, WebSessionEntity session, String deviceId, AuthUserDto user) {
        AuthResponse body = new AuthResponse(
                pair.accessToken(),
                pair.accessExp().toString(),
                pair.refreshToken(),
                pair.refreshExp().toString(),
                session.getSessionId().toString(),
                deviceId,
                user
        );
        return new LoginResult(body, pair.refreshToken(), session.getSessionId().toString(), deviceId);
    }

    private WebSessionEntity resolveOrCreateSession(
            UUID userId,
            String sessionHeader,
            String sessionCookie,
            String deviceId,
            RequestInfo requestInfo
    ) {
        UUID sessionId = parseUuidOrNull(firstNonBlank(sessionHeader, sessionCookie));
        if (sessionId != null) {
            var active = webSessionService.getActive(sessionId);
            if (active.isPresent() && active.get().getUserId().equals(userId)) {
                webSessionService.touch(sessionId);
                return active.get();
            }
        }
        return webSessionService.createSession(userId, deviceId, requestInfo.userAgent(), requestInfo.ip());
    }

    private AuthUserDto toUserDto(UserEntity user, LocalCredentialEntity cred, UserProfileEntity profile) {
        UserProfileEntity p = profile == null ? defaultProfile(user.getId()) : profile;
        Object socialLinks = parseJsonOrFallback(p.getSocialLinks(), List.of());
        return new AuthUserDto(
                user.getId().toString(),
                user.getUsername(),
                cred.getEmail(),
                cred.isEmailVerified(),
                p.getDisplayName(),
                p.getBio(),
                p.getLocation(),
                p.getWebsite(),
                p.getBirthDate() == null ? null : p.getBirthDate().toString(),
                ProfileUrlBuilder.avatarUrl(user.getId(), p.getAvatarRev()),
                ProfileUrlBuilder.bannerUrl(user.getId(), p.getBannerRev()),
                ProfileUrlBuilder.wallpaperUrl(user.getId(), p.getWallpaperRev()),
                p.getProfileTheme(),
                p.getCustomThemeColor1(),
                p.getCustomThemeColor2(),
                p.getUserStatus(),
                p.getCustomStatus(),
                socialLinks
        );
    }

    private Object parseJsonOrFallback(String json, Object fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return fallback;
        }
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials"));
    }

    private UserProfileEntity defaultProfile(UUID userId) {
        UserProfileEntity p = new UserProfileEntity();
        p.setUserId(userId);
        p.setDisplayName("");
        p.setBio("");
        p.setLocation("");
        p.setWebsite("");
        p.setAvatarRev(0);
        p.setBannerRev(0);
        p.setWallpaperRev(0);
        p.setSocialLinks("[]");
        return p;
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base;
        int n = 1;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + "_" + (++n);
        }
        return candidate;
    }

    private boolean loginAllowed(String ip) {
        long now = System.currentTimeMillis();
        long[] state = loginRateLimit.compute(ip, (k, v) -> {
            if (v == null || now - v[0] > props.getLoginRateLimitWindowMs()) {
                return new long[]{now, 0};
            }
            v[1]++;
            return v;
        });
        return state[1] <= props.getLoginRateLimitMax();
    }

    private void loginClear(String ip) {
        loginRateLimit.remove(ip);
    }

    private UUID parseUuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private String toIso(OffsetDateTime ts) {
        return ts == null ? null : ts.toInstant().toString();
    }

    public record LoginResult(
            AuthResponse responseBody,
            String refreshTokenForCookie,
            String sessionIdForCookie,
            String deviceIdForCookie
    ) {
    }
}
