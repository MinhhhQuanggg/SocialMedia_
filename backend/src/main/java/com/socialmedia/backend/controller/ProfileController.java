package com.socialmedia.backend.controller;

import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;
    private final AuthService authService;

    public ProfileController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
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

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Integer userId = requireUserId();
        User u = userService.getMyProfile(userId);
        return ResponseEntity.ok(userService.toProfileResponse(u));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable("userId") Integer userId) {
        User u = userService.findById(userId);
        if (u == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userService.toProfileResponse(u));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody Map<String, String> body) {
        Integer userId = Integer.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()
        );
        User u = userService.updateMyProfile(userId, body);
        return ResponseEntity.ok(userService.toProfileResponse(u));
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        Integer userId = requireUserId();
        String current = body.get("currentPassword");
        String next = body.get("newPassword");
        authService.changePassword(userId, current, next);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage();
        int status = 400;
        if ("UNAUTHORIZED".equals(msg)) status = 401;
        if ("USER_NOT_FOUND".equals(msg)) status = 404;
        return ResponseEntity.status(status).body(Map.of("error", msg));
    }
}
