package com.socialmedia.backend.controller;

import com.socialmedia.backend.entity.Notification;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.NotificationRepository;
import com.socialmedia.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                  UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ===== AUTH =====
    private Integer requireUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();

        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        Object principal = a.getPrincipal();
        if (principal == null) throw new RuntimeException("UNAUTHORIZED");

        try {
            return Integer.valueOf(principal.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("UNAUTHORIZED");
        }
    }

    private User requireUser() {
        return userRepository.findById(requireUserId())
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    }

    // ===== GET NOTIFICATIONS =====
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = requireUser();

        List<Notification> list =
                notificationRepository
                        .findByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size))
                        .getContent();

        // Trả JSON gọn với thông tin actor và post
        
        List<Map<String, Object>> res = list.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("notificationId", n.getNotificationId());
            m.put("type", n.getType());
            m.put("message", n.getMessage());
            m.put("isRead", n.getIsRead());
            m.put("createdAt", n.getCreatedAt());
            
            // Thông tin người thực hiện hành động
            if (n.getActor() != null) {
                Map<String, Object> actor = new HashMap<>();
                actor.put("userId", n.getActor().getUserId());
                actor.put("userName", n.getActor().getUserName());
                actor.put("avatarUrl", n.getActor().getAvatarUrl());
                m.put("actor", actor);
            }
            
            // Thông tin bài viết
            if (n.getPost() != null) {
                Map<String, Object> post = new HashMap<>();
                post.put("postId", n.getPost().getPostId());
                post.put("content", n.getPost().getContent());
                post.put("imageUrl", n.getPost().getImageUrl());
                m.put("post", post);
            }
            
            return m;
        }).toList();

        return ResponseEntity.ok(res);
    }


    // ===== MARK AS READ =====
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer id) {
        Integer userId = requireUserId();
        int updated = notificationRepository.markAsRead(id, userId);
        if (updated == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOTIFICATION_NOT_FOUND"));
        }
        return ResponseEntity.ok(Map.of("message", "READ"));
    }

    @GetMapping("/count")
    public ResponseEntity<?> countUnread() {
        User user = requireUser();
        long count = notificationRepository.countUnreadByUser(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }


    // ===== SIMPLE ERROR HANDLER =====
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handle(RuntimeException ex) {
        if ("UNAUTHORIZED".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED"));
        }
        if ("USER_NOT_FOUND".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "USER_NOT_FOUND"));
        }
        if ("NOTIFICATION_NOT_FOUND".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOTIFICATION_NOT_FOUND"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
