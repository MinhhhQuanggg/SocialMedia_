package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.Comment;
import com.socialmedia.backend.entity.Post;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.CommentRepository;
import com.socialmedia.backend.repository.PostRepository;
import com.socialmedia.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.socialmedia.backend.websocket.ChatWebSocketHandler chatWebSocketHandler;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          com.socialmedia.backend.websocket.ChatWebSocketHandler chatWebSocketHandler) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    // ===== CREATE =====
    @Transactional
    public Comment create(Integer userId, Integer postId, Map<String, String> body) {
        User commenter = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        // ✅ lấy post kèm user owner để khỏi lazy
        Post post = postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));

        String content = body.get("content");
        if (content == null || content.trim().isBlank()) {
            throw new RuntimeException("CONTENT_REQUIRED");
        }

        Comment c = new Comment();
        c.setUser(commenter);
        c.setPost(post);
        c.setContent(content.trim());

        Comment saved = commentRepository.save(c);

        // ✅ tạo notification cho chủ bài viết (nếu không tự comment bài mình)
        User owner = post.getUser();
        if (owner != null && owner.getUserId() != null && !owner.getUserId().equals(userId)) {
            notificationService.createCommentNotification(owner, commenter, post, content.trim());
        }

        // Broadcast to WebSocket clients about the new comment
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "NEW_COMMENT");
            Map<String, Object> payload = new HashMap<>(this.toResponse(saved));
            payload.put("postId", saved.getPost() != null ? saved.getPost().getPostId() : null);
            event.put("data", payload);
            chatWebSocketHandler.broadcastEvent(event);
        } catch (Exception ex) {
            // ignore websocket failures
            ex.printStackTrace();
        }

        return saved;
    }

    // ===== READ =====
    @Transactional(readOnly = true)
    public List<Comment> getByPost(Integer postId) {
        return commentRepository.findByPostId(postId);
    }

    // ===== UPDATE =====
    @Transactional
    public Comment update(Integer userId, Integer commentId, Map<String, String> body) {
        Comment c = commentRepository.findByIdWithUser(commentId)
                .orElseThrow(() -> new RuntimeException("COMMENT_NOT_FOUND"));

        if (!c.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("FORBIDDEN");
        }

        String content = body.get("content");
        if (content == null || content.trim().isBlank()) {
            throw new RuntimeException("CONTENT_REQUIRED");
        }

        c.setContent(content.trim());
        return commentRepository.save(c);
    }

    // ===== DELETE =====
    @Transactional
    public void delete(Integer userId, Integer commentId) {
        Comment c = commentRepository.findByIdWithUser(commentId)
                .orElseThrow(() -> new RuntimeException("COMMENT_NOT_FOUND"));

        // allow deletion by comment owner OR by the owner of the post
        boolean isCommentOwner = c.getUser() != null && c.getUser().getUserId().equals(userId);
        boolean isPostOwner = c.getPost() != null && c.getPost().getUser() != null && c.getPost().getUser().getUserId().equals(userId);

        if (!isCommentOwner && !isPostOwner) {
            throw new RuntimeException("FORBIDDEN");
        }

        commentRepository.delete(c);

        // Broadcast deletion event
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "DELETE_COMMENT");
            event.put("data", Map.of("commentId", commentId, "postId", c.getPost() != null ? c.getPost().getPostId() : null));
            chatWebSocketHandler.broadcastEvent(event);
        } catch (Exception ex) {
            // ignore
            ex.printStackTrace();
        }
    }

    // ===== RESPONSE MAP =====
    public Map<String, Object> toResponse(Comment c) {
        Map<String, Object> res = new HashMap<>();
        res.put("commentId", c.getCommentId());
        res.put("content", c.getContent());
        res.put("createdAt", c.getCreatedAt());
        res.put("updateAt", c.getUpdateAt());

        Map<String, Object> author = new HashMap<>();
        author.put("userId", c.getUser().getUserId());
        author.put("userName", c.getUser().getUserName());
        author.put("avatarUrl", c.getUser().getAvatarUrl());
        res.put("user", author);

        return res;
    }
}
