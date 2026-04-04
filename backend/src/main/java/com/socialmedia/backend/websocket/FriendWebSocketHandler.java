package com.socialmedia.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FriendWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(FriendWebSocketHandler.class);

    private final ObjectMapper objectMapper;

    // userId -> session
    private final ConcurrentHashMap<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public FriendWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Gửi thông báo event tới user cụ thể
     * Event types: friend_request_sent, friend_request_received, friend_accepted, friend_removed
     */
    public void sendToUser(Integer userId, Map<String, Object> event) {
        WebSocketSession session = sessions.get(userId);
        safeSend(session, event);
    }

    /**
     * Gửi event tới tất cả users (broadcast)
     */
    public void broadcastEvent(Map<String, Object> event) {
        logger.info("Broadcasting friend event: {}", event.get("type"));
        sessions.values().forEach(s -> safeSend(s, event));
    }

    private Integer getUserId(WebSocketSession session) {
        Object uid = session.getAttributes().get("userId");
        return uid == null ? null : (Integer) uid;
    }

    private void safeSend(WebSocketSession session, Object payload) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (Exception e) {
            logger.error("Error sending WebSocket message:", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer userId = getUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("UNAUTHORIZED"));
            return;
        }

        // Close old session for same user
        WebSocketSession oldSession = sessions.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            oldSession.close(CloseStatus.NORMAL);
        }

        sessions.put(userId, session);
        logger.info("Friend WebSocket connected for user: {}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Integer userId = getUserId(session);
        if (userId != null) {
            sessions.remove(userId);
            logger.info("Friend WebSocket disconnected for user: {}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Integer userId = getUserId(session);
        if (userId != null) {
            sessions.remove(userId);
            logger.error("Friend WebSocket transport error for user {}: {}", userId, exception.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Friend WebSocket chỉ nhận thông báo từ server, không xử lý message từ client
        logger.debug("Received message on friend WebSocket (not processed): {}", message.getPayload());
    }
}
