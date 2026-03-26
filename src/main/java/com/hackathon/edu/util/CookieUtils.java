package com.hackathon.edu.util;

import com.hackathon.edu.config.AppSecurityProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieUtils {
    private CookieUtils() {
    }

    public static void addRefreshCookie(HttpServletResponse response, AppSecurityProperties props, String refreshToken, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from("refresh", refreshToken)
                .path("/api")
                .httpOnly(true)
                .secure(props.getCookies().isSecure())
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(maxAgeSeconds));
        if (props.getCookies().getDomain() != null && !props.getCookies().getDomain().isBlank()) {
            b.domain(props.getCookies().getDomain());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
    }

    public static void addSessionCookie(HttpServletResponse response, AppSecurityProperties props, String sessionId, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from("session_id", sessionId)
                .path("/")
                .httpOnly(true)
                .secure(props.getCookies().isSecure())
                .sameSite("Strict")
                .maxAge(Duration.ofSeconds(maxAgeSeconds));
        if (props.getCookies().getDomain() != null && !props.getCookies().getDomain().isBlank()) {
            b.domain(props.getCookies().getDomain());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
    }

    public static void addDeviceCookie(HttpServletResponse response, AppSecurityProperties props, String deviceId, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from("device_id", deviceId)
                .path("/")
                .httpOnly(true)
                .secure(props.getCookies().isSecure())
                .sameSite("Strict")
                .maxAge(Duration.ofSeconds(maxAgeSeconds));
        if (props.getCookies().getDomain() != null && !props.getCookies().getDomain().isBlank()) {
            b.domain(props.getCookies().getDomain());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
    }

    public static void clearAuthCookies(HttpServletResponse response, AppSecurityProperties props) {
        response.addHeader(HttpHeaders.SET_COOKIE, expired("refresh", "/api", "Lax", props).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expired("refresh", "/", "Lax", props).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expired("session_id", "/", "Strict", props).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expired("device_id", "/", "Strict", props).toString());
    }

    private static ResponseCookie expired(String name, String path, String sameSite, AppSecurityProperties props) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, "deleted")
                .path(path)
                .httpOnly(true)
                .secure(props.getCookies().isSecure())
                .sameSite(sameSite)
                .maxAge(Duration.ZERO);
        if (props.getCookies().getDomain() != null && !props.getCookies().getDomain().isBlank()) {
            b.domain(props.getCookies().getDomain());
        }
        return b.build();
    }
}
