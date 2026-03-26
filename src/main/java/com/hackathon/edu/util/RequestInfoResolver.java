package com.hackathon.edu.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestInfoResolver {
    private RequestInfoResolver() {
    }

    public static RequestInfo resolve(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return new RequestInfo(parts[0].trim(), userAgent(request));
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return new RequestInfo(xRealIp.trim(), userAgent(request));
        }
        return new RequestInfo(request.getRemoteAddr(), userAgent(request));
    }

    private static String userAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua == null ? "" : ua;
    }
}
