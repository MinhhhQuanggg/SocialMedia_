package com.socialmedia.backend.controller;

import com.socialmedia.backend.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    /**
     * Lấy danh sách lời mời kết bạn đang chờ
     * GET /api/friends/requests/pending
     */
    @GetMapping("/requests/pending")
    public ResponseEntity<?> getPendingRequests(Authentication auth) {
        Integer userId = Integer.parseInt(auth.getName());
        List<Map<String, Object>> requests = friendService.getPendingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Lấy danh sách bạn bè
     * GET /api/friends/list
     */
    @GetMapping("/list")
    public ResponseEntity<?> getFriendsList(Authentication auth) {
        Integer userId = Integer.parseInt(auth.getName());
        List<Map<String, Object>> friends = friendService.getFriendsList(userId);
        return ResponseEntity.ok(friends);
    }

    /**
     * Lấy gợi ý kết bạn
     * GET /api/friends/suggestions?limit=10
     */
    @GetMapping("/suggestions")
    public ResponseEntity<?> getSuggestions(
            Authentication auth,
            @RequestParam(defaultValue = "10") int limit) {
        Integer userId = Integer.parseInt(auth.getName());
        List<Map<String, Object>> suggestions = friendService.getSuggestions(userId, limit);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Gửi lời mời kết bạn
     * POST /api/friends/send/{targetUserId}
     */
    @PostMapping("/send/{targetUserId}")
    public ResponseEntity<?> sendFriendRequest(
            Authentication auth,
            @PathVariable Integer targetUserId) {
        Integer userId = Integer.parseInt(auth.getName());
        Map<String, Object> result = friendService.sendFriendRequest(userId, targetUserId);
        return ResponseEntity.ok(result);
    }

    /**
     * Chấp nhận lời mời kết bạn
     * POST /api/friends/{requestFromUserId}/accept
     */
    @PostMapping("/{requestFromUserId}/accept")
    public ResponseEntity<?> acceptFriendRequest(
            Authentication auth,
            @PathVariable Integer requestFromUserId) {
        Integer userId = Integer.parseInt(auth.getName());
        Map<String, Object> result = friendService.acceptFriendRequest(userId, requestFromUserId);
        return ResponseEntity.ok(result);
    }

    /**
     * Từ chối lời mời kết bạn
     * POST /api/friends/{requestFromUserId}/reject
     */
    @PostMapping("/{requestFromUserId}/reject")
    public ResponseEntity<?> rejectFriendRequest(
            Authentication auth,
            @PathVariable Integer requestFromUserId) {
        Integer userId = Integer.parseInt(auth.getName());
        Map<String, Object> result = friendService.rejectFriendRequest(userId, requestFromUserId);
        return ResponseEntity.ok(result);
    }

    /**
     * Xóa bạn bè
     * DELETE /api/friends/{friendUserId}
     */
    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<?> removeFriend(
            Authentication auth,
            @PathVariable Integer friendUserId) {
        Integer userId = Integer.parseInt(auth.getName());
        Map<String, Object> result = friendService.removeFriend(userId, friendUserId);
        return ResponseEntity.ok(result);
    }

    /**
     * Block user
     * POST /api/friends/{targetUserId}/block
     */
    @PostMapping("/{targetUserId}/block")
    public ResponseEntity<?> blockUser(
            Authentication auth,
            @PathVariable Integer targetUserId) {
        Integer userId = Integer.parseInt(auth.getName());
        Map<String, Object> result = friendService.blockUser(userId, targetUserId);
        return ResponseEntity.ok(result);
    }

    /**
     * Kiểm tra trạng thái bạn bè
     * GET /api/friends/{targetUserId}/status
     */
    @GetMapping("/{targetUserId}/status")
    public ResponseEntity<?> getFriendshipStatus(
            Authentication auth,
            @PathVariable Integer targetUserId) {
        Integer userId = Integer.parseInt(auth.getName());
        String status = friendService.getFriendshipStatus(userId, targetUserId);
        return ResponseEntity.ok(Map.of("status", status));
    }

    /**
     * Get friend list of a specific user (with privacy check)
     * GET /api/friends/profile/{userId}
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfileFriends(@PathVariable Integer userId) {
        Integer viewerId = null;
        try {
            // Optional: get current user if authenticated
            // In a real implementation, extract from Authentication if present
        } catch (Exception e) {
            // Anonymous viewer
        }

        // Check privacy
        if (!friendService.canViewFriendList(viewerId, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN_BY_PRIVACY"));
        }

        List<Map<String, Object>> friends = friendService.getFriendsList(userId);
        return ResponseEntity.ok(friends);
    }
}
