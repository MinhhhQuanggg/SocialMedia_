package com.socialmedia.backend.controller;

import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        // Accept both `userName` and `username` (frontend uses `username`)
        String userName = body.getOrDefault("userName", body.get("username"));
        String email = body.get("email");
        String password = body.get("password");

        User u = authService.register(userName, email, password);

        // trả về tối thiểu, không trả pass
        return ResponseEntity.ok(Map.of(
                "userId", u.getUserId(),
                "userName", u.getUserName(),
                "email", u.getEmail(),
                "createdAt", String.valueOf(u.getCreatedAt())
        ));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        // Accept `login` or `username` or `email`
        String login = body.get("login"); // email hoặc userName
        if (login == null) login = body.get("username");
        if (login == null) login = body.get("email");
        String password = body.get("password");

        String token = authService.login(login, password);
        User user = authService.findByUsernameOrEmail(login);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("type", "Bearer");
        response.put("token", token);
        
        // Add user info
        if (user != null) {
            response.put("userId", user.getUserId());
            response.put("userName", user.getUserName());
            response.put("email", user.getEmail());
            response.put("avatarUrl", user.getAvatarUrl());
            if (user.getRole() != null) {
                response.put("role", user.getRole().getRoleName());
            } else {
                response.put("role", "USER");
            }
        }

        return ResponseEntity.ok(response);
    }
}
