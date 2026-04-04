package com.socialmedia.backend.controller;

import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ===== USERS =====

    // GET /api/admin/users?q=abc
    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestParam(name = "q", required = false) String q) {
        List<User> list = adminService.listUsers(q);
        return ResponseEntity.ok(list.stream().map(adminService::toUserResponse).toList());
    }

    // PUT /api/admin/users/{id}/ban
    @PutMapping("/users/{id}/ban")
    public ResponseEntity<?> ban(@PathVariable("id") Integer userId) {
        return ResponseEntity.ok(adminService.setUserStatus(userId, false));
    }

    // PUT /api/admin/users/{id}/unban
    @PutMapping("/users/{id}/unban")
    public ResponseEntity<?> unban(@PathVariable("id") Integer userId) {
        return ResponseEntity.ok(adminService.setUserStatus(userId, true));
    }

    // ===== POSTS =====

    // GET /api/admin/posts
    @GetMapping("/posts")
    public ResponseEntity<?> listPosts() {
        return ResponseEntity.ok(adminService.listAllPosts());
    }

    // DELETE /api/admin/posts/{id}
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> deletePost(@PathVariable("id") Integer postId) {
        adminService.adminDeletePost(postId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // ===== COMMENTS =====

    // GET /api/admin/comments
    @GetMapping("/comments")
    public ResponseEntity<?> listComments() {
        return ResponseEntity.ok(adminService.listAllComments());
    }

    // DELETE /api/admin/comments/{id}
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable("id") Integer commentId) {
        adminService.adminDeleteComment(commentId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // ===== STATS =====

    // GET /api/admin/stats
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(adminService.stats());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        if ("USER_NOT_FOUND".equals(ex.getMessage())) {
            return ResponseEntity.status(404).body(Map.of("error", "USER_NOT_FOUND"));
        }
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }

}
