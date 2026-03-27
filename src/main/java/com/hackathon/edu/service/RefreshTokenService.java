package com.hackathon.edu.service;

import com.hackathon.edu.config.AppSecurityProperties;
import com.hackathon.edu.entity.RefreshTokenEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.RefreshTokenRepository;
import com.hackathon.edu.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom RND = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AppSecurityProperties props;

    @Transactional
    public TokenPair issue(UUID userId, String ip) {
        UUID familyId = UUID.randomUUID();
        return rotateInternal(userId, familyId, null, ip);
    }

    @Transactional
    public TokenPair rotate(RefreshTokenEntity old, String ip) {
        return rotateInternal(old.getUserId(), old.getFamilyId(), old.getTokenId(), ip);
    }

    @Transactional
    public void revokeFamily(UUID familyId) {
        refreshTokenRepository.revokeFamily(familyId, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public ParsedRefreshToken parse(String presented) {
        if (presented == null || presented.isBlank() || !presented.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "bad token");
        }
        String[] parts = presented.split("\\.", 2);
        try {
            return new ParsedRefreshToken(UUID.fromString(parts[0]), parts[1]);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "bad token");
        }
    }

    public boolean matchesHash(UUID tokenId, String raw, byte[] expectedHash) {
        byte[] actual = hashPresented(tokenId, raw);
        return MessageDigest.isEqual(actual, expectedHash);
    }

    public byte[] hashPresented(UUID tokenId, String raw) {
        return sha256((tokenId + "." + raw).getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] sha256(byte[] value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TokenPair rotateInternal(UUID userId, UUID familyId, UUID oldTokenId, String ip) {
        UUID newTokenId = UUID.randomUUID();
        String raw = newRawToken();
        String refresh = newTokenId + "." + raw;
        byte[] hash = hashPresented(newTokenId, raw);
        OffsetDateTime refreshExp = OffsetDateTime.now(ZoneOffset.UTC).plusDays(props.getRefreshTtlDays());

        RefreshTokenEntity row = new RefreshTokenEntity();
        row.setTokenId(newTokenId);
        row.setFamilyId(familyId);
        row.setUserId(userId);
        row.setTokenHash(hash);
        row.setIp(ip);
        row.setExpiresAt(refreshExp);
        refreshTokenRepository.save(row);

        if (oldTokenId != null) {
            refreshTokenRepository.markReplaced(oldTokenId, newTokenId);
        }

        String access = jwtService.issue(userId, newTokenId);
        Instant accessExp = Instant.ofEpochSecond(jwtService.verify(access).expEpochSec());
        return new TokenPair(access, refresh, accessExp, refreshExp.toInstant(), newTokenId);
    }

    private String newRawToken() {
        byte[] b = new byte[32];
        RND.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public record ParsedRefreshToken(UUID tokenId, String raw) {
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Instant accessExp,
            Instant refreshExp,
            UUID refreshTokenId
    ) {
    }
}
