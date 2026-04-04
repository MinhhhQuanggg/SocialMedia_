package com.socialmedia.backend.controller;

import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Get user ID from SecurityContext
    private Integer requireUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("UserController.requireUserId() - Authentication: " + a);
        if (a != null) {
            System.out.println("  - isAuthenticated: " + a.isAuthenticated());
            System.out.println("  - Principal: " + a.getPrincipal());
        }
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            System.out.println("  - UNAUTHORIZED!");
            throw new RuntimeException("UNAUTHORIZED");
        }
        Object principal = a.getPrincipal();
        if (principal == null) throw new RuntimeException("UNAUTHORIZED");
        try {
            Integer userId = Integer.valueOf(principal.toString());
            System.out.println("  - userId: " + userId);
            return userId;
        } catch (NumberFormatException e) {
            throw new RuntimeException("UNAUTHORIZED");
        }
    }

    /**
     * Get privacy settings for current user
     */
    @GetMapping("/privacy")
    public ResponseEntity<?> getPrivacySettings() {
        System.out.println("GET /api/settings/privacy called");
        try {
            Integer userId = requireUserId();
            System.out.println("Getting privacy settings for user: " + userId);
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "USER_NOT_FOUND"));
            }

            Map<String, String> settings = new HashMap<>();
            settings.put("privacyPosts", user.getPrivacyPosts() != null ? user.getPrivacyPosts() : "FRIENDS");
            settings.put("privacyFriendRequests", user.getPrivacyFriendRequests() != null ? user.getPrivacyFriendRequests() : "EVERYONE");
            settings.put("privacyFriendList", user.getPrivacyFriendList() != null ? user.getPrivacyFriendList() : "ONLY_ME");

            System.out.println("Returning privacy settings: " + settings);
            return ResponseEntity.ok(settings);
        } catch (RuntimeException e) {
            System.out.println("Error in getPrivacySettings: " + e.getMessage());
            if ("UNAUTHORIZED".equals(e.getMessage())) {
                return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
            }
            throw e;
        }
    }

    /**
     * Update privacy settings for current user
     */
    @PutMapping("/privacy")
    public ResponseEntity<?> updatePrivacySettings(@RequestBody Map<String, String> settings) {
        try {
            Integer userId = requireUserId();
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "USER_NOT_FOUND"));
            }

            if (settings.containsKey("privacyPosts")) {
                user.setPrivacyPosts(settings.get("privacyPosts"));
            }
            if (settings.containsKey("privacyFriendRequests")) {
                user.setPrivacyFriendRequests(settings.get("privacyFriendRequests"));
            }
            if (settings.containsKey("privacyFriendList")) {
                user.setPrivacyFriendList(settings.get("privacyFriendList"));
            }

            userService.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Privacy settings updated");
            response.put("privacyPosts", user.getPrivacyPosts());
            response.put("privacyFriendRequests", user.getPrivacyFriendRequests());
            response.put("privacyFriendList", user.getPrivacyFriendList());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if ("UNAUTHORIZED".equals(e.getMessage())) {
                return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
            }
            throw e;
        }
    }
}
