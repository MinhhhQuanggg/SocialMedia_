package com.socialmedia.backend.controller;

import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.UserRepository;
import com.socialmedia.backend.service.ReactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class ReactionController {

    private final ReactionService reactionService;
    private final UserRepository userRepository;

    public ReactionController(ReactionService reactionService, UserRepository userRepository) {
        this.reactionService = reactionService;
        this.userRepository = userRepository;
    }

    private Integer requireUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();

        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        Object principal = a.getPrincipal();
        if (principal == null) {
            throw new RuntimeException("UNAUTHORIZED");
        }

        try {
            return Integer.valueOf(principal.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("UNAUTHORIZED");
        }
    }

    private User requireUser() {
        Integer userId = requireUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    }


    // ===== REACT (like, love, haha, wow, sad, angry) =====
    @PostMapping("/{postId}/react")
    public ResponseEntity<?> react(@PathVariable Integer postId, @RequestParam String type) {
        reactionService.reactPost(postId, requireUser(), type);
        return ResponseEntity.ok(Map.of("message", "Reacted", "type", type));
    }

    // ===== UNREACT =====
    @DeleteMapping("/{postId}/react")
    public ResponseEntity<?> unreact(@PathVariable Integer postId) {
        reactionService.unreactPost(postId, requireUser());
        return ResponseEntity.ok(Map.of("message", "Unreacted"));
    }


    // ===== COUNT ALL REACTIONS =====
    @GetMapping("/{postId}/reactions/count")
    public ResponseEntity<?> countAll(@PathVariable Integer postId) {
        return ResponseEntity.ok(
                Map.of("count", reactionService.countReactions(postId))
        );
    }

    // ===== COUNT BY TYPE =====
    @GetMapping("/{postId}/reactions/count/{type}")
    public ResponseEntity<?> countByType(@PathVariable Integer postId, @PathVariable String type) {
        return ResponseEntity.ok(
                Map.of("count", reactionService.countReactionsByType(postId, type))
        );
    }


    // ===== CHECK MY REACTION TYPE =====
    @GetMapping("/{postId}/reactions/me")
    public ResponseEntity<?> myReaction(@PathVariable Integer postId) {
        String type = reactionService.getMyReactionType(postId, requireUser());
        // Return "none" instead of null to avoid Map.of NullPointerException
        return ResponseEntity.ok(Map.of("type", type != null ? type : "none"));
    }

    // ===== TEMP ERROR HANDLER (nếu chưa có global) =====
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
