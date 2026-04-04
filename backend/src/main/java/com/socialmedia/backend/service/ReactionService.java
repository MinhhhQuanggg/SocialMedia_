package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.Reaction;
import com.socialmedia.backend.entity.Post;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.ReactionRepository;
import com.socialmedia.backend.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    public ReactionService(ReactionRepository reactionRepository,
                       PostRepository postRepository,
                       NotificationService notificationService) {
        this.reactionRepository = reactionRepository;
        this.postRepository = postRepository;
        this.notificationService = notificationService;
    }


    // REACT (like, love, haha, wow, sad, angry...)
    public void reactPost(Integer postId, User user, String type) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Chỉ cho phép 1 reaction/post/user, update nếu đã có
        Reaction reaction = reactionRepository.findByPostAndUser(post, user).orElse(null);
        if (reaction != null) {
            // Nếu cùng loại thì bỏ (unreact)
            if (reaction.getType().equals(type)) {
                reactionRepository.delete(reaction);
                return;
            }
            // Đổi loại reaction
            reaction.setType(type);
            reaction.setCreatedAt(LocalDateTime.now());
            reactionRepository.save(reaction);
        } else {
            // Tạo mới
            reaction = new Reaction();
            reaction.setPost(post);
            reaction.setUser(user);
            reaction.setType(type);
            reaction.setCreatedAt(LocalDateTime.now());
            reactionRepository.save(reaction);
        }

        // ===== NOTIFICATION WHEN REACT =====
        User owner = post.getUser();
        if (owner != null && owner.getUserId() != null
                && user.getUserId() != null
                && !owner.getUserId().equals(user.getUserId())) {
            notificationService.createLikeNotification(owner, user, post);
        }
    }

        // UNREACT (bỏ mọi loại reaction)
        public void unreactPost(Integer postId, User user) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        Reaction reaction = reactionRepository.findByPostAndUser(post, user)
            .orElse(null);
        if (reaction != null) {
            reactionRepository.delete(reaction);
        }
        }


    // COUNT ALL REACTIONS
    public long countReactions(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return reactionRepository.countByPost(post);
    }

    // COUNT BY TYPE
    public long countReactionsByType(Integer postId, String type) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return reactionRepository.countByPostAndType(post, type);
    }

    // CHECK ME (loại nào)
    public String getMyReactionType(Integer postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        Reaction reaction = reactionRepository.findByPostAndUser(post, user).orElse(null);
        return reaction != null ? reaction.getType() : null;
    }
}
