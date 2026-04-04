package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.Comment;
import com.socialmedia.backend.entity.Post;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.CommentRepository;
import com.socialmedia.backend.repository.ReactionRepository;
import com.socialmedia.backend.repository.PostRepository;
import com.socialmedia.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository likeRepository;

    public AdminService(UserRepository userRepository,
                        PostRepository postRepository,
                        CommentRepository commentRepository,
                        ReactionRepository likeRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }

    // ===== USERS =====

    @Transactional(readOnly = true)
    public List<User> listUsers(String q) {
        if (q == null) q = "";
        q = q.trim();
        if (q.isBlank()) return userRepository.findAllWithRole();
        return userRepository.searchWithRole(q);
    }

    @Transactional
    public Map<String, Object> setUserStatus(Integer userId, boolean enabled) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        u.setStatus(enabled);
        User saved = userRepository.save(u);

        // ✅ build response ngay trong transaction
        return Map.of(
                "updated", true,
                "user", toUserResponse(saved)
        );
    }

    // ===== POSTS =====

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllPosts() {
        List<Post> posts = postRepository.adminListAll();
        return posts.stream().map(this::toPostResponse).toList();
    }

    @Transactional
    public void adminDeletePost(Integer postId) {
        Post p = postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));

        // Xóa comment, like trước để tránh FK
        commentRepository.deleteByPostId(postId);
        likeRepository.deleteByPostId(postId);

        postRepository.delete(p);
    }

    // ===== COMMENTS =====

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllComments() {
        List<Comment> comments = commentRepository.findAll();
        return comments.stream().map(this::toCommentResponse).toList();
    }

    @Transactional
    public void adminDeleteComment(Integer commentId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("COMMENT_NOT_FOUND"));
        commentRepository.delete(c);
    }

    // ===== STATS =====

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long users = userRepository.count();
        long posts = postRepository.countAll();
        long comments = commentRepository.countAll();
        long likes = likeRepository.countAll();

        return Map.of(
                "users", users,
                "posts", posts,
                "comments", comments,
                "likes", likes
        );
    }

    // ===== HELPERS RESPONSE =====

    public Map<String, Object> toUserResponse(User u) {
        Map<String, Object> res = new HashMap<>();
        res.put("userId", u.getUserId());
        res.put("userName", u.getUserName());
        res.put("email", u.getEmail());
        res.put("avatarUrl", u.getAvatarUrl());
        res.put("bio", u.getBio());
        res.put("status", u.getStatus()); // true/false
        res.put("createdAt", u.getCreatedAt());

        if (u.getRole() != null) {
            res.put("role", u.getRole().getRoleName());
        } else {
            res.put("role", "USER");
        }
        return res;
    }

    public Map<String, Object> toPostResponse(Post p) {
        Map<String, Object> res = new HashMap<>();
        res.put("postId", p.getPostId());
        res.put("content", p.getContent());
        res.put("createdAt", p.getCreatedAt());
        if (p.getUser() != null) {
            res.put("author", p.getUser().getUserName());
            res.put("userId", p.getUser().getUserId());
            res.put("authorAvatar", p.getUser().getAvatarUrl());
        }
        return res;
    }

    public Map<String, Object> toCommentResponse(Comment c) {
        Map<String, Object> res = new HashMap<>();
        res.put("commentId", c.getCommentId());
        res.put("content", c.getContent());
        res.put("createdAt", c.getCreatedAt());
        if (c.getUser() != null) {
            res.put("author", c.getUser().getUserName());
            res.put("userId", c.getUser().getUserId());
            res.put("authorAvatar", c.getUser().getAvatarUrl());
        }
        if (c.getPost() != null) {
            res.put("postId", c.getPost().getPostId());
        }
        return res;
    }
}
