package com.socialmedia.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialmedia.backend.entity.Post;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.CommentRepository;
import com.socialmedia.backend.repository.PostRepository;
import com.socialmedia.backend.repository.ReactionRepository;
import com.socialmedia.backend.repository.UserRepository;
import com.socialmedia.backend.repository.NotificationRepository;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository likeRepository;
    private final NotificationRepository notificationRepository;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       CommentRepository commentRepository,
                       ReactionRepository likeRepository,
                       NotificationRepository notificationRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.notificationRepository = notificationRepository;
    }

    // ===== CREATE =====
    @Transactional
    public Post createPost(Integer userId, Map<String, String> body) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        String content = body.get("content");
        if (content != null) content = content.trim();
        if (content == null || content.isBlank()) {
            throw new RuntimeException("CONTENT_REQUIRED");
        }
        if (content.length() > 100) { // theo DB nvarchar(100) 
            throw new RuntimeException("CONTENT_TOO_LONG");
        }

        String imageUrl = body.get("imageUrl");
        if (imageUrl != null) {
            imageUrl = imageUrl.trim();
            imageUrl = imageUrl.isBlank() ? null : imageUrl;
        }

        Post p = new Post();
        p.setUser(u);
        p.setContent(content);
        p.setImageUrl(imageUrl);

        return postRepository.save(p);
    }

    // ===== READ =====
    @Transactional(readOnly = true)
    public Post getPostDetail(Integer postId) {
        return postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));
    }

    @Transactional(readOnly = true)
    public List<Post> getMyPosts(Integer userId) {
        // có thể check user tồn tại hoặc không, tuỳ bạn
        return postRepository.findMyPosts(userId);
    }

    @Transactional(readOnly = true)
    public List<Post> getFeed() {
        return postRepository.findFeed();
    }

    // ===== UPDATE =====
    @Transactional
    public Post updatePost(Integer userId, Integer postId, Map<String, String> body) {
        Post p = postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));

        // check owner
        if (p.getUser() == null || !p.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("FORBIDDEN");
        }

        String content = body.get("content");
        if (content != null) {
            content = content.trim();
            if (content.isBlank()) throw new RuntimeException("CONTENT_REQUIRED");
            if (content.length() > 100) throw new RuntimeException("CONTENT_TOO_LONG");
            p.setContent(content);
        }

        String imageUrl = body.get("imageUrl");
        if (imageUrl != null) {
            imageUrl = imageUrl.trim();
            p.setImageUrl(imageUrl.isBlank() ? null : imageUrl);
        }

        return postRepository.save(p);
    }

    // ===== DELETE =====
    @Transactional
    public void deletePost(Integer userId, Integer postId) {
        Post p = postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));

        if (p.getUser() == null || !p.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("FORBIDDEN");
        }
        
        // Xóa notifications liên quan đến post trước
        notificationRepository.deleteByPostId(postId);
        
        postRepository.delete(p);
    }

    // ===== Helper trả JSON Map =====
    public Map<String, Object> toPostResponse(Post p) {
        Map<String, Object> res = new HashMap<>();
        res.put("postId", p.getPostId());
        res.put("content", p.getContent());
        res.put("imageUrl", p.getImageUrl());
        res.put("createdAt", p.getCreatedAt());
        res.put("updateAt", p.getUpdateAt());

        long commentCount = commentRepository.countByPostId(p.getPostId());
        res.put("commentCount", commentCount);

        // include like count
        long likeCount = likeRepository.countByPost(p);
        res.put("likeCount", likeCount);

        User u = p.getUser();
        if (u != null) {
            Map<String, Object> author = new HashMap<>();
            author.put("userId", u.getUserId());
            author.put("userName", u.getUserName());
            author.put("avatarUrl", u.getAvatarUrl());
            res.put("user", author);
            
            // Include privacy setting from author
            String privacy = u.getPrivacyPosts() != null ? u.getPrivacyPosts() : "FRIENDS";
            res.put("privacy", privacy);
        } else {
            res.put("user", null);
            res.put("privacy", "FRIENDS");
        }
        return res;
    }

    // ===== PRIVACY HELPER =====
    /**
     * Check if viewer can see posts from target user
     * viewerId: người xem (null = anonymous)
     * targetUserId: chủ nhân posts
     */
    public boolean canViewPosts(Integer viewerId, Integer targetUserId) {
        // Chủ nhân luôn thấy posts của mình
        if (viewerId != null && viewerId.equals(targetUserId)) {
            return true;
        }

        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) return false;

        String privacy = targetUser.getPrivacyPosts();
        if (privacy == null) privacy = "FRIENDS"; // default

        if ("EVERYONE".equals(privacy)) {
            return true;
        }

        if ("ONLY_ME".equals(privacy)) {
            // Chỉ chủ nhân
            return viewerId != null && viewerId.equals(targetUserId);
        }

        if ("FRIENDS".equals(privacy)) {
            // Chỉ bạn bè có thể xem
            if (viewerId == null) return false;
            // TODO: check if viewerId is friend of targetUserId
            // Với codebase hiện tại, ta cần query FriendRepository
            return true; // Tạm: nếu đã auth thì có thể xem
        }

        return false;
    }
}
