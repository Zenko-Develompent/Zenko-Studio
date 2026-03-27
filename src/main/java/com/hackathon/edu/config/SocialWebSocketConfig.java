package com.hackathon.edu.config;

import com.hackathon.edu.websocket.WsAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class SocialWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final AppChatProperties appChatProperties;
    private final WsAuthChannelInterceptor wsAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(appChatProperties.getWsEndpoint())
                .setAllowedOriginPatterns(appChatProperties.wsAllowedOriginPatternsArray());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(appChatProperties.brokerDestinationPrefixesArray());
        registry.setApplicationDestinationPrefixes(appChatProperties.getApplicationDestinationPrefix());
        registry.setUserDestinationPrefix(appChatProperties.getUserDestinationPrefix());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(wsAuthChannelInterceptor);
    }
}
