package com.socialmedia.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "type")
    private String type; // "LIKE", "COMMENT", "FRIEND_REQUEST", ...

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // người nhận thông báo

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor; // người thực hiện hành động (like, comment)

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post; // bài viết liên quan (nếu có)

    @Column(name = "message", length = 500)
    private String message; // nội dung thông báo

    // ===== GETTER & SETTER =====

    public Integer getNotificationId() { return notificationId; }
    public void setNotificationId(Integer notificationId) { this.notificationId = notificationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
