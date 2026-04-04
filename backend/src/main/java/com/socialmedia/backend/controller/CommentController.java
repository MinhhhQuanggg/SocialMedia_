package com.socialmedia.backend.controller;

import com.socialmedia.backend.entity.Comment;
import com.socialmedia.backend.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    private Integer requireUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("UNAUTHORIZED");
        }
        return Integer.valueOf(a.getPrincipal().toString());
    }

    // CREATE
    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable Integer postId,
            @RequestBody Map<String, String> body
    ) {
        Integer userId = requireUserId();
        Comment c = commentService.create(userId, postId, body);
        return ResponseEntity.ok(commentService.toResponse(c));
    }

    // LIST
    @GetMapping
    public ResponseEntity<?> list(@PathVariable Integer postId) {
        List<Comment> list = commentService.getByPost(postId);
        return ResponseEntity.ok(list.stream().map(commentService::toResponse).toList());
    }

    // UPDATE
    @PutMapping("/{commentId}")
    public ResponseEntity<?> update(
            @PathVariable Integer postId,
            @PathVariable Integer commentId,
            @RequestBody Map<String, String> body
    ) {
        Integer userId = requireUserId();
        Comment c = commentService.update(userId, commentId, body);
        return ResponseEntity.ok(commentService.toResponse(c));
    }

    // DELETE
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> delete(
            @PathVariable Integer postId,
            @PathVariable Integer commentId
    ) {
        Integer userId = requireUserId();
        commentService.delete(userId, commentId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handle(RuntimeException ex) {
        if ("UNAUTHORIZED".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED"));
        }
        throw ex;
    }
}
