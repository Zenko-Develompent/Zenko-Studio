package com.hackathon.edu.service;

import com.hackathon.edu.config.AppSecurityProperties;
import com.hackathon.edu.dto.AuthResponse;
import com.hackathon.edu.dto.AuthUserDto;
import com.hackathon.edu.dto.LoginRequest;
import com.hackathon.edu.dto.LogoutRequest;
import com.hackathon.edu.dto.RefreshRequest;
import com.hackathon.edu.dto.RegisterResponse;
import com.hackathon.edu.dto.WebSessionDto;
import com.hackathon.edu.entity.RefreshTokenEntity;
import com.hackathon.edu.entity.RoleEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.entity.WebSessionEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.RefreshTokenRepository;
import com.hackathon.edu.repository.RoleRepository;
import com.hackathon.edu.repository.UserRepository;
import com.hackathon.edu.security.JwtService;
import com.hackathon.edu.security.PasswordHasher;
import com.hackathon.edu.util.PasswordPolicy;
import com.hackathon.edu.util.ProfileUrlBuilder;
import com.hackathon.edu.util.RequestInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final WebSessionService webSessionService;
    private final JwtService jwtService;
    private final AppSecurityProperties props;

    private final ConcurrentHashMap<String, long[]> loginRateLimit = new ConcurrentHashMap<>();

    @Transactional
    public RegisterResponse register(String usernameRaw, String password, Integer ageRaw, String roleRaw) {
        String username = usernameRaw == null ? null : usernameRaw.trim();

        if (username == null || !username.matches("[A-Za-z0-9_]{3,16}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_username");
        }
        if (!PasswordPolicy.accept(password)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "weak_password");
        }
        if (ageRaw == null || ageRaw < 1 || ageRaw > 120) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_age");
        }

        String role = normalizeRoleInput(roleRaw);
        if (role == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_role");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "conflict");
        }

        String uniqueUsername = ensureUniqueUsername(username);
        UserEntity user = new UserEntity();
        user.setUsername(uniqueUsername);
        user.setPassword(PasswordHasher.hash(password.toCharArray()));
        if (user.getXp() == null) {
            user.setXp(0);
        }
        if (user.getLevel() == null) {
            user.setLevel(0);
        }
        user.setBirthDate(LocalDate.now(ZoneOffset.UTC).minusYears(ageRaw));
        user.setRole(resolveRole(role));
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "conflict");
        }

        return new RegisterResponse(
                user.getUserId().toString(),
                user.getUsername(),
                ageRaw,
                role
        );
    }

    @Transactional
    public RegisterResponse upsertAdmin(String usernameRaw, String password, Integer ageRaw) {
        String username = usernameRaw == null ? null : usernameRaw.trim();

        if (username == null || !username.matches("[A-Za-z0-9_]{3,16}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_username");
        }
        if (!PasswordPolicy.accept(password)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "weak_password");
        }

        int age = ageRaw == null ? 30 : ageRaw;
        if (age < 1 || age > 120) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_age");
        }

        RoleEntity adminRole = ensureRoleByDbName("ADMIN");

        UserEntity user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            user = new UserEntity();
            user.setUsername(username);
        }
        user.setPassword(PasswordHasher.hash(password.toCharArray()));
        if (user.getXp() == null) {
            user.setXp(0);
        }
        if (user.getLevel() == null) {
            user.setLevel(0);
        }
        user.setBirthDate(LocalDate.now(ZoneOffset.UTC).minusYears(age));
        user.setRole(adminRole);
        user = userRepository.saveAndFlush(user);

        revokeTokensAndSessions(user.getUserId());

        return new RegisterResponse(
                user.getUserId().toString(),
                user.getUsername(),
                age,
                "admin"
        );
    }

    @Transactional
    public LoginResult login(LoginRequest request, RequestInfo requestInfo) {
        String ip = requestInfo.ip();
        if (!loginAllowed(ip)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts");
        }

        String identifier = firstNonBlank(request.id(), request.username());
        String password = request.password();
        if (identifier == null || password == null || password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "bad_request");
        }

        UserEntity user = findUserByIdentifier(identifier);
        if (user == null || !PasswordHasher.verify(password.toCharArray(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
        }

        loginClear(ip);

        UUID userId = user.getUserId();
        String deviceId = UUID.randomUUID().toString();
        WebSessionEntity session = webSessionService.createSession(userId, deviceId, requestInfo.userAgent(), requestInfo.ip());
        RefreshTokenService.TokenPair pair = refreshTokenService.issue(userId, deviceId, requestInfo.ip(), requestInfo.userAgent());
        return buildLoginResult(pair, session, deviceId, toUserDto(user));
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
        String deviceId = firstNonBlank(deviceHeader, deviceCookie, UUID.randomUUID().toString());
        WebSessionEntity session = resolveOrCreateSession(userId, sessionHeader, sessionCookie, deviceId, requestInfo);
        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(row, deviceId, requestInfo.ip(), requestInfo.userAgent());
        UserEntity user = requireUser(userId);
        return buildLoginResult(pair, session, deviceId, toUserDto(user));
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

    private LoginResult buildLoginResult(
            RefreshTokenService.TokenPair pair,
            WebSessionEntity session,
            String deviceId,
            AuthUserDto user
    ) {
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

    private AuthUserDto toUserDto(UserEntity user) {
        UUID userId = user.getUserId();
        LocalDate birthDate = user.getBirthDate();
        return new AuthUserDto(
                userId.toString(),
                user.getUsername(),
                user.getUsername(),
                birthDate == null ? null : birthDate.toString(),
                birthDate == null ? null : calculateAge(birthDate),
                toApiRole(user.getRole()),
                ProfileUrlBuilder.avatarUrl(userId, 0),
                List.of()
        );
    }

    private UserEntity findUserByIdentifier(String identifier) {
        UUID userId = parseUuidOrNull(identifier);
        if (userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        return userRepository.findByUsernameIgnoreCase(identifier).orElse(null);
    }

    private RoleEntity resolveRole(String role) {
        String dbRoleName = switch (role) {
            case "parent" -> "PARENT";
            case "student" -> "STUDENT";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_role");
        };
        return ensureRoleByDbName(dbRoleName);
    }

    private RoleEntity ensureRoleByDbName(String dbRoleName) {
        return roleRepository.findByNameIgnoreCase(dbRoleName)
                .orElseGet(() -> {
                    RoleEntity roleEntity = new RoleEntity();
                    roleEntity.setUserId(UUID.randomUUID());
                    roleEntity.setName(dbRoleName);
                    try {
                        return roleRepository.saveAndFlush(roleEntity);
                    } catch (DataIntegrityViolationException ex) {
                        return roleRepository.findByNameIgnoreCase(dbRoleName)
                                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));
                    }
                });
    }

    private void revokeTokensAndSessions(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .map(RefreshTokenEntity::getFamilyId)
                .distinct()
                .forEach(familyId -> refreshTokenRepository.revokeFamily(familyId, now));

        webSessionService.listActive(userId).forEach(s -> webSessionService.deactivateForUser(s.getSessionId(), userId));
    }

    private String normalizeRoleInput(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return null;
        }
        String value = rawRole.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "parent" -> "parent";
            case "student" -> "student";
            default -> null;
        };
    }

    private String toApiRole(RoleEntity roleEntity) {
        if (roleEntity == null || roleEntity.getName() == null || roleEntity.getName().isBlank()) {
            return null;
        }
        String value = roleEntity.getName().trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "parent" -> "parent";
            case "student" -> "student";
            default -> value;
        };
    }

    private Integer calculateAge(LocalDate birthDate) {
        int years = Period.between(birthDate, LocalDate.now(ZoneOffset.UTC)).getYears();
        return Math.max(years, 0);
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials"));
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
