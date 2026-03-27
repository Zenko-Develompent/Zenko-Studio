package com.hackathon.edu.controller;

import com.hackathon.edu.config.AppSecurityProperties;
import com.hackathon.edu.dto.LoginRequest;
import com.hackathon.edu.dto.LogoutRequest;
import com.hackathon.edu.dto.ProfileDTO;
import com.hackathon.edu.dto.RefreshRequest;
import com.hackathon.edu.dto.RegisterRequest;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.util.CookieUtils;
import com.hackathon.edu.util.RequestInfoResolver;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AppSecurityProperties properties;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request.username(), request.password(), request.age(), request.role()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        var result = authService.login(request, RequestInfoResolver.resolve(httpRequest));
        CookieUtils.addRefreshCookie(httpResponse, properties, result.refreshTokenForCookie(), authService.refreshMaxAgeSeconds());
        return ResponseEntity.ok(result.responseBody());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = "refresh", required = false) String refreshCookie,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        var result = authService.refresh(
                request,
                refreshCookie,
                RequestInfoResolver.resolve(httpRequest)
        );
        CookieUtils.addRefreshCookie(httpResponse, properties, result.refreshTokenForCookie(), authService.refreshMaxAgeSeconds());
        return ResponseEntity.ok(result.responseBody());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody(required = false) LogoutRequest request,
            @CookieValue(name = "refresh", required = false) String refreshCookie,
            HttpServletResponse httpResponse
    ) {
        authService.logout(request, refreshCookie);
        CookieUtils.clearAuthCookies(httpResponse, properties);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/profile")
    public ProfileDTO.ProfileResponse profile(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return authService.getProfile(authService.requireUserIdFromAccessHeader(authorizationHeader));
    }
}
