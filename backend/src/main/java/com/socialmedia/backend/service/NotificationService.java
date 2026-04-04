package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.Notification;
import com.socialmedia.backend.entity.Post;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.NotificationRepository;
import com.socialmedia.backend.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public NotificationService(NotificationRepository notificationRepository,
                              @Lazy ChatWebSocketHandler chatWebSocketHandler) {
        this.notificationRepository = notificationRepository;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    // Tạo thông báo khi có người like bài viết
    public Notification createLikeNotification(User receiver, User actor, Post post) {
        Notification n = new Notification();
        n.setUser(receiver);
        n.setActor(actor);
        n.setPost(post);
        n.setType("LIKE");
        n.setMessage(actor.getUserName() + " đã thích bài viết của bạn");
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(n);
        
        // Gửi real-time notification qua WebSocket
        sendNotificationToUser(saved);
        
        return saved;
    }

    // Tạo thông báo khi có người comment bài viết
    public Notification createCommentNotification(User receiver, User actor, Post post, String commentContent) {
        Notification n = new Notification();
        n.setUser(receiver);
        n.setActor(actor);
        n.setPost(post);
        n.setType("COMMENT");
        // Truncate comment nếu quá dài
        String shortContent = commentContent.length() > 50 
            ? commentContent.substring(0, 50) + "..." 
            : commentContent;
        n.setMessage(actor.getUserName() + " đã bình luận: " + shortContent);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(n);
        
        // Gửi real-time notification qua WebSocket
        sendNotificationToUser(saved);
        
        return saved;
    }
    
    // Gửi notification qua WebSocket
    private void sendNotificationToUser(Notification notification) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "NEW_NOTIFICATION");
            
            Map<String, Object> data = new HashMap<>();
            data.put("notificationId", notification.getNotificationId());
            data.put("type", notification.getType());
            data.put("message", notification.getMessage());
            data.put("isRead", notification.getIsRead());
            data.put("createdAt", notification.getCreatedAt());
            
            if (notification.getActor() != null) {
                Map<String, Object> actor = new HashMap<>();
                actor.put("userId", notification.getActor().getUserId());
                actor.put("userName", notification.getActor().getUserName());
                actor.put("avatarUrl", notification.getActor().getAvatarUrl());
                data.put("actor", actor);
            }
            
            if (notification.getPost() != null) {
                Map<String, Object> post = new HashMap<>();
                post.put("postId", notification.getPost().getPostId());
                post.put("content", notification.getPost().getContent());
                post.put("imageUrl", notification.getPost().getImageUrl());
                data.put("post", post);
            }
            
            event.put("data", data);
            
            // Gửi đến user cụ thể
            if (notification.getUser() != null) {
                chatWebSocketHandler.sendToUser(notification.getUser().getUserId(), event);
            }
        } catch (Exception ex) {
            // Log error but don't fail the notification creation
            ex.printStackTrace();
        }
    }
}
