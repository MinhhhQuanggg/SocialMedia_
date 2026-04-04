package com.socialmedia.backend.config;

import com.socialmedia.backend.websocket.ChatWebSocketHandler;
import com.socialmedia.backend.websocket.FriendWebSocketHandler;
import com.socialmedia.backend.websocket.UserIdHandshakeInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final FriendWebSocketHandler friendWebSocketHandler;
    private final UserIdHandshakeInterceptor userIdHandshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler, 
                          FriendWebSocketHandler friendWebSocketHandler,
                          UserIdHandshakeInterceptor userIdHandshakeInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.friendWebSocketHandler = friendWebSocketHandler;
        this.userIdHandshakeInterceptor = userIdHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(userIdHandshakeInterceptor)
                .setAllowedOrigins("*");
        
        registry.addHandler(friendWebSocketHandler, "/ws/friends")
                .addInterceptors(userIdHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
