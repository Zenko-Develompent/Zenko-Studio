package com.hackathon.edu.service;

import com.hackathon.edu.config.AppSecurityProperties;
import com.hackathon.edu.entity.WebSessionEntity;
import com.hackathon.edu.repository.WebSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSessionService {
    private final WebSessionRepository webSessionRepository;
    private final AppSecurityProperties props;

    @Transactional
    public WebSessionEntity createSession(UUID userId, String deviceId, String userAgent, String ipAddress) {
        WebSessionEntity session = new WebSessionEntity();
        session.setUserId(userId);
        session.setDeviceId(deviceId);
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        session.setLastActivityAt(now);
        session.setExpiresAt(now.plusSeconds(props.getSessionTtlSec()));
        return webSessionRepository.save(session);
    }

    @Transactional
    public void touch(UUID sessionId) {
        webSessionRepository.touch(sessionId, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void deactivate(UUID sessionId) {
        webSessionRepository.deactivate(sessionId);
    }

    @Transactional
    public void deactivateForUser(UUID sessionId, UUID userId) {
        webSessionRepository.deactivateForUser(sessionId, userId);
    }

    public Optional<WebSessionEntity> getActive(UUID sessionId) {
        return webSessionRepository.findBySessionIdAndActiveTrue(sessionId)
                .filter(s -> s.getExpiresAt() != null && s.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)));
    }

    public List<WebSessionEntity> listActive(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return webSessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .stream()
                .filter(s -> s.getExpiresAt() != null && s.getExpiresAt().isAfter(now))
                .toList();
    }
}
