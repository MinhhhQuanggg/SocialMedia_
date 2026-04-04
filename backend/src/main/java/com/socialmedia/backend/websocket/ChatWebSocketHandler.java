package com.socialmedia.backend.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.backend.entity.Message;
import com.socialmedia.backend.entity.Group;
import com.socialmedia.backend.service.MessageService;
import com.socialmedia.backend.service.GroupService;
import com.socialmedia.backend.service.ReactionService;
import com.socialmedia.backend.repository.UserRepository;
import com.socialmedia.backend.entity.User;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final ReactionService reactionService;
    private final UserRepository userRepository;
    private final GroupService groupService;

    // userId -> session
    private final ConcurrentHashMap<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(MessageService messageService, ObjectMapper objectMapper, ReactionService reactionService, UserRepository userRepository, GroupService groupService) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.reactionService = reactionService;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    // Send event to all connected sessions
    public void broadcastEvent(Map<String, Object> event) {
        try {
            logger.info("Broadcasting WS event: {}", event.get("type"));
        } catch (Exception ignored) {}
        sessions.values().forEach(s -> safeSend(s, event));
    }

    // Send event to specific user
    public void sendToUser(Integer userId, Map<String, Object> event) {
        WebSocketSession s = sessions.get(userId);
        safeSend(s, event);
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
            // đừng throw để khỏi làm văng handler
            e.printStackTrace();
        }
    }

    /**
     * Broadcast event to group members
     */
    private void broadcastToGroup(Integer groupId, Map<String, Object> event) {
        try {
            Group group = groupService.getGroup(groupId);
            List<Integer> memberIds = group.getMembers().stream()
                    .map(m -> m.getUser().getUserId())
                    .toList();
            
            memberIds.forEach(memberId -> sendToUser(memberId, event));
            logger.info("Broadcast to group {}: {} members", groupId, memberIds.size());
        } catch (Exception e) {
            logger.error("Error broadcasting to group {}", groupId, e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer userId = getUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("UNAUTHORIZED"));
            return;
        }

        // Nếu user login 2 tab, đóng session cũ cho gọn
        WebSocketSession old = sessions.put(userId, session);
        if (old != null && old.isOpen() && old != session) {
            try { old.close(CloseStatus.NORMAL.withReason("reconnected")); } catch (Exception ignored) {}
        }

        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "CONNECTED");
        ack.put("userId", userId);
        safeSend(session, ack);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        Integer fromUserId = getUserId(session);
        if (fromUserId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("type", "ERROR");
            err.put("message", "UNAUTHORIZED");
            safeSend(session, err);
            return;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    textMessage.getPayload(),
                    new TypeReference<Map<String, Object>>() {}
            );

            String type = payload.get("type") == null ? null : payload.get("type").toString();
            
            // Handle media chunks - deprecated, use REST API instead
            if ("MEDIA_CHUNK".equals(type)) {
                logger.warn("MEDIA_CHUNK via WebSocket deprecated, use /api/chat/send-media endpoint");
                return;
            }

            // Handle group message
            if ("GROUP_MESSAGE".equals(type)) {
                Integer groupId = payload.get("groupId") == null ? null : Integer.valueOf(payload.get("groupId").toString());
                String content = payload.get("content") == null ? null : payload.get("content").toString();

                if (groupId == null || content == null) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("type", "ERROR");
                    err.put("message", "groupId and content required");
                    safeSend(session, err);
                    return;
                }

                // Check if user is member of group
                if (!groupService.isMember(groupId, fromUserId)) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("type", "ERROR");
                    err.put("message", "User is not member of this group");
                    safeSend(session, err);
                    return;
                }

                // Save message
                User sender = userRepository.findById(fromUserId).orElse(null);
                Group group = groupService.getGroup(groupId);
                
                Message message = new Message();
                message.setSender(sender);
                message.setGroup(group);
                message.setContent(content);
                message.setStatus(true);
                
                Message saved = messageService.saveMessage(message);

                // Broadcast to group
                Map<String, Object> event = new HashMap<>();
                event.put("type", "GROUP_MESSAGE");
                event.put("groupId", groupId);
                event.put("data", messageService.toMessageResponse(saved));
                broadcastToGroup(groupId, event);
                return;
            }
            
            if ("REACT_POST".equals(type)) {
                // Xử lý sự kiện reaction từ client, lưu DB
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                if (data == null) data = new HashMap<>();
                data.put("userId", fromUserId);
                Integer postId = data.get("postId") == null ? null : Integer.valueOf(data.get("postId").toString());
                String reactType = data.get("type") == null ? "like" : data.get("type").toString();
                if (postId != null && fromUserId != null) {
                    User user = userRepository.findById(fromUserId).orElse(null);
                    if (user != null) {
                        reactionService.reactPost(postId, user, reactType);
                    }
                }
                // Broadcast cho tất cả client (hoặc chỉ các client liên quan nếu muốn)
                Map<String, Object> event = new HashMap<>();
                event.put("type", "REACT_POST");
                event.put("data", data);
                broadcastEvent(event);
                return;
            }

            // Mặc định: xử lý như message chat (yêu cầu toUserId)
            Integer toUserId = payload.get("toUserId") == null ? null : Integer.valueOf(payload.get("toUserId").toString());
            String content = payload.get("content") == null ? null : payload.get("content").toString();
            String imageUrl = payload.get("imageUrl") == null ? null : payload.get("imageUrl").toString();
            String videoUrl = payload.get("videoUrl") == null ? null : payload.get("videoUrl").toString();

            // body null-safe (không dùng Map.of)
            Map<String, String> body = new HashMap<>();
            body.put("content", content == null ? "" : content);
            body.put("imageUrl", imageUrl == null ? "" : imageUrl);
            body.put("videoUrl", videoUrl == null ? "" : videoUrl);

            // ✅ save DB + validate
            Message saved = messageService.send(fromUserId, toUserId, body);

            Map<String, Object> event = new HashMap<>();
            event.put("type", "NEW_MESSAGE");
            event.put("data", messageService.toMessageResponse(saved)); // null-safe

            // ✅ push receiver
            WebSocketSession receiverSession = sessions.get(toUserId);
            safeSend(receiverSession, event);

            // ✅ ack sender
            safeSend(session, event);

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> err = new HashMap<>();
            err.put("type", "ERROR");
            err.put("message", e.getClass().getSimpleName());
            err.put("detail", e.getMessage());
            safeSend(session, err);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = getUserId(session);
        if (userId == null) return;

        // chỉ remove nếu session hiện tại đúng là session đang map
        sessions.computeIfPresent(userId, (k, current) -> current == session ? null : current);
    }
}
