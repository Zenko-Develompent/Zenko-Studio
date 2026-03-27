package com.hackathon.edu.service;

import com.hackathon.edu.config.AppSecurityProperties;
import com.hackathon.edu.dto.AuthResponse;
import com.hackathon.edu.dto.AuthUserDto;
import com.hackathon.edu.dto.LoginRequest;
import com.hackathon.edu.dto.LogoutRequest;
import com.hackathon.edu.dto.ProfileDTO;
import com.hackathon.edu.dto.RefreshRequest;
import com.hackathon.edu.dto.RegisterResponse;
import com.hackathon.edu.entity.AchievementUserEntity;
import com.hackathon.edu.entity.RefreshTokenEntity;
import com.hackathon.edu.entity.RoleEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.AchievementUserRepository;
import com.hackathon.edu.repository.RefreshTokenRepository;
import com.hackathon.edu.repository.RoleRepository;
import com.hackathon.edu.repository.UserRepository;
import com.hackathon.edu.security.JwtService;
import com.hackathon.edu.security.PasswordHasher;
import com.hackathon.edu.util.PasswordPolicy;
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
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AchievementUserRepository achievementUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
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
        if (user.getCoins() == null) {
            user.setCoins(0);
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
        if (user.getCoins() == null) {
            user.setCoins(0);
        }
        user.setBirthDate(LocalDate.now(ZoneOffset.UTC).minusYears(age));
        user.setRole(adminRole);
        user = userRepository.saveAndFlush(user);

        revokeTokens(user.getUserId());

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
        RefreshTokenService.TokenPair pair = refreshTokenService.issue(userId, requestInfo.ip());
        return buildLoginResult(pair, toUserDto(user));
    }

    @Transactional
    public LoginResult refresh(
            RefreshRequest request,
            String refreshCookie,
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
        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(row, requestInfo.ip());
        UserEntity user = requireUser(userId);
        return buildLoginResult(pair, toUserDto(user));
    }

    @Transactional
    public void logout(
            LogoutRequest request,
            String refreshCookie
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
    }

    public UUID requireUserIdFromAccessHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        return jwtService.verify(token).userId();
    }

    @Transactional(readOnly = true)
    public UUID requireAdminUserIdFromAccessHeader(String authorizationHeader) {
        UUID userId = requireUserIdFromAccessHeader(authorizationHeader);
        UserEntity user = requireUser(userId);
        String role = user.getRole() == null || user.getRole().getName() == null
                ? ""
                : user.getRole().getName().trim().toLowerCase(Locale.ROOT);
        if (!"admin".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden");
        }
        return userId;
    }

    public long refreshMaxAgeSeconds() {
        return props.getRefreshTtlDays() * 86400L;
    }

    public ProfileDTO.ProfileResponse getProfile(UUID userId) {
        UserEntity user = requireUser(userId);
        int xp = user.getXp() == null ? 0 : user.getXp();
        int level = user.getLevel() == null ? 0 : user.getLevel();
        int coins = user.getCoins() == null ? 0 : user.getCoins();

        var achievements = achievementUserRepository.findByUser_UserIdOrderByCreatedAtAsc(userId).stream()
                .map(AchievementUserEntity::getAchievement)
                .filter(a -> a != null && a.getAchievementId() != null && a.getName() != null)
                .map(a -> new ProfileDTO.AchievementItem(a.getAchievementId(), a.getName()))
                .toList();

        return new ProfileDTO.ProfileResponse(user.getUsername(), xp, level, coins, achievements);
    }

    private LoginResult buildLoginResult(
            RefreshTokenService.TokenPair pair,
            AuthUserDto user
    ) {
        AuthResponse body = new AuthResponse(
                pair.accessToken(),
                pair.accessExp().toString(),
                pair.refreshToken(),
                pair.refreshExp().toString(),
                user
        );
        return new LoginResult(body, pair.refreshToken());
    }

    private AuthUserDto toUserDto(UserEntity user) {
        UUID userId = user.getUserId();
        LocalDate birthDate = user.getBirthDate();
        return new AuthUserDto(
                userId.toString(),
                user.getUsername(),
                birthDate == null ? null : birthDate.toString(),
                birthDate == null ? null : calculateAge(birthDate),
                toApiRole(user.getRole()),
                user.getXp() == null ? 0 : user.getXp(),
                user.getLevel() == null ? 0 : user.getLevel(),
                user.getCoins() == null ? 0 : user.getCoins()
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

    private void revokeTokens(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .map(RefreshTokenEntity::getFamilyId)
                .distinct()
                .forEach(familyId -> refreshTokenRepository.revokeFamily(familyId, now));
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

    public record LoginResult(
            AuthResponse responseBody,
            String refreshTokenForCookie
    ) {
    }
}
