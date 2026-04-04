package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.Post;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.PostRepository;
import com.socialmedia.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostService postService; // reuse toPostResponse

    public SearchService(PostRepository postRepository,
                         UserRepository userRepository,
                         PostService postService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postService = postService;
    }

    private String normalizeQ(String q) {
        if (q == null) return null;
        q = q.trim();
        return q.isBlank() ? null : q;
    }

    // ===== SEARCH POSTS =====
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchPosts(String q) {
        q = normalizeQ(q);
        List<Post> list = postRepository.searchPosts(q);
        return list.stream().map(postService::toPostResponse).toList();
    }

    // ===== SEARCH USERS =====
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchUsers(String q) {
        q = normalizeQ(q);
        List<User> list = userRepository.searchUsers(q);
        return list.stream().map(this::toUserResponse).toList();
    }

    // style Map giống PostService
    public Map<String, Object> toUserResponse(User u) {
        Map<String, Object> res = new HashMap<>();
        res.put("userId", u.getUserId());
        res.put("userName", u.getUserName());
        res.put("email", u.getEmail());
        res.put("avatarUrl", u.getAvatarUrl());
        res.put("bio", u.getBio());
        return res;
    }
}
