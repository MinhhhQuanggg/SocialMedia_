package com.socialmedia.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialmedia.backend.entity.Post;
import com.socialmedia.backend.service.PostService;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

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

    // CREATE (cần login)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Integer userId = requireUserId();
        Post p = postService.createPost(userId, body);
        return ResponseEntity.ok(postService.toPostResponse(p));
    }

    // FEED (tuỳ bạn: muốn ai cũng xem thì permitAll endpoint này ở SecurityConfig)
    @GetMapping("/feed")
    public ResponseEntity<?> feed() {
        List<Post> list = postService.getFeed();
        return ResponseEntity.ok(list.stream().map(postService::toPostResponse).toList());
    }

    // MY POSTS (cần login)
    @GetMapping("/me")
    public ResponseEntity<?> myPosts() {
        Integer userId = requireUserId();
        List<Post> list = postService.getMyPosts(userId);
        return ResponseEntity.ok(list.stream().map(postService::toPostResponse).toList());
    }

    // DETAIL (tuỳ bạn: public hay auth)
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable("id") Integer postId) {
        Post p = postService.getPostDetail(postId);
        return ResponseEntity.ok(postService.toPostResponse(p));
    }

    // GET PROFILE POSTS (public hay auth - check privacy)
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> profilePosts(@PathVariable("userId") Integer targetUserId) {
        Integer viewerId = null;
        try {
            viewerId = requireUserId();
        } catch (RuntimeException e) {
            // Anonymous viewer
        }

        // Check privacy
        if (!postService.canViewPosts(viewerId, targetUserId)) {
            System.out.println("Privacy denied: viewerId=" + viewerId + ", targetUserId=" + targetUserId);
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN_BY_PRIVACY"));
        }

        System.out.println("Privacy allowed: viewerId=" + viewerId + ", targetUserId=" + targetUserId);
        List<Post> list = postService.getMyPosts(targetUserId);
        System.out.println("Returning " + list.size() + " posts");
        return ResponseEntity.ok(list.stream().map(postService::toPostResponse).toList());
    }

    // UPDATE (cần login + check owner trong service)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Integer postId,
            @RequestBody Map<String, String> body
    ) {
        Integer userId = requireUserId();
        Post p = postService.updatePost(userId, postId, body);
        return ResponseEntity.ok(postService.toPostResponse(p));
    }

    // DELETE (cần login + check owner trong service)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer postId) {
        Integer userId = requireUserId();
        postService.deletePost(userId, postId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // (khuyên) map RuntimeException -> 401 nhanh gọn cho UNAUTHORIZED
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        if ("UNAUTHORIZED".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "UNAUTHORIZED"));
        }
        throw ex;
    }
}
