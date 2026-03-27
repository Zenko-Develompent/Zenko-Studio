package com.hackathon.edu.websocket;

import com.hackathon.edu.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WsAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token == null || token.isBlank()) {
                throw new MessagingException("unauthorized");
            }
            UUID userId = jwtService.verify(token).userId();
            accessor.setUser(new StompUserPrincipal(userId.toString()));
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authorization = firstHeader(accessor, "Authorization");
        if (authorization == null) {
            authorization = firstHeader(accessor, "authorization");
        }
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring("Bearer ".length()).trim();
        }
        if (authorization != null && !authorization.isBlank()) {
            return authorization.trim();
        }
        String accessToken = firstHeader(accessor, "access_token");
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken.trim();
        }
        String token = firstHeader(accessor, "token");
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        return null;
    }

    private String firstHeader(StompHeaderAccessor accessor, String headerName) {
        List<String> values = accessor.getNativeHeader(headerName);
        if (values == null || values.isEmpty()) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
