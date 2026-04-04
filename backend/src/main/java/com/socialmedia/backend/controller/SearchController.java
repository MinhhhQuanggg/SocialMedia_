package com.socialmedia.backend.controller;

import com.socialmedia.backend.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    // Nếu bạn muốn bắt buộc login mới search -> bật requireUserId() trong 2 endpoint
    private Integer requireUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("UNAUTHORIZED");
        }
        Object principal = a.getPrincipal();
        if (principal == null) throw new RuntimeException("UNAUTHORIZED");
        return Integer.valueOf(principal.toString());
    }

    @GetMapping("/posts")
    public ResponseEntity<?> searchPosts(@RequestParam(name = "q", required = false) String q) {
        requireUserId(); // mở dòng này nếu muốn auth
        return ResponseEntity.ok(searchService.searchPosts(q));
    }

    @GetMapping("/users")
    public ResponseEntity<?> searchUsers(@RequestParam(name = "q", required = false) String q) {
        requireUserId(); // mở dòng này nếu muốn auth
        return ResponseEntity.ok(searchService.searchUsers(q));
    }

    // (optional) y chang PostController: map UNAUTHORIZED -> 401
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        if ("UNAUTHORIZED".equals(ex.getMessage())) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
        }
        throw ex;
    }
}
