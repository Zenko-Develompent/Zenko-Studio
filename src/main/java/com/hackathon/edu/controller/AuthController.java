package com.hackathon.edu.controller;

import com.hackathon.edu.config.AppSecurityProperties;
import com.hackathon.edu.dto.LoginRequest;
import com.hackathon.edu.dto.LogoutRequest;
import com.hackathon.edu.dto.RefreshRequest;
import com.hackathon.edu.dto.RegisterRequest;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.util.CookieUtils;
import com.hackathon.edu.util.RequestInfoResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AppSecurityProperties properties;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request.username(), request.email(), request.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        var result = authService.login(request, RequestInfoResolver.resolve(httpRequest));
        CookieUtils.addRefreshCookie(httpResponse, properties, result.refreshTokenForCookie(), authService.refreshMaxAgeSeconds());
        CookieUtils.addSessionCookie(httpResponse, properties, result.sessionIdForCookie(), authService.sessionMaxAgeSeconds());
        CookieUtils.addDeviceCookie(httpResponse, properties, result.deviceIdForCookie(), authService.sessionMaxAgeSeconds());
        return ResponseEntity.ok(result.responseBody());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = "refresh", required = false) String refreshCookie,
            @CookieValue(name = "session_id", required = false) String sessionCookie,
            @CookieValue(name = "device_id", required = false) String deviceCookie,
            @RequestHeader(name = "X-Session-ID", required = false) String sessionHeader,
            @RequestHeader(name = "X-Device-ID", required = false) String deviceHeader,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        var result = authService.refresh(
                request,
                refreshCookie,
                sessionHeader,
                deviceHeader,
                sessionCookie,
                deviceCookie,
                RequestInfoResolver.resolve(httpRequest)
        );
        CookieUtils.addRefreshCookie(httpResponse, properties, result.refreshTokenForCookie(), authService.refreshMaxAgeSeconds());
        CookieUtils.addSessionCookie(httpResponse, properties, result.sessionIdForCookie(), authService.sessionMaxAgeSeconds());
        CookieUtils.addDeviceCookie(httpResponse, properties, result.deviceIdForCookie(), authService.sessionMaxAgeSeconds());
        return ResponseEntity.ok(result.responseBody());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody(required = false) LogoutRequest request,
            @CookieValue(name = "refresh", required = false) String refreshCookie,
            @CookieValue(name = "session_id", required = false) String sessionCookie,
            @RequestHeader(name = "X-Session-ID", required = false) String sessionHeader,
            HttpServletResponse httpResponse
    ) {
        authService.logout(request, refreshCookie, sessionHeader, sessionCookie);
        CookieUtils.clearAuthCookies(httpResponse, properties);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> sessions(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @CookieValue(name = "session_id", required = false) String currentSessionCookie
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return ResponseEntity.ok(Map.of("sessions", authService.listWebSessions(userId, currentSessionCookie)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable("sessionId") UUID sessionId
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        authService.revokeWebSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
