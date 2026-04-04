package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.Friend;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.FriendRepository;
import com.socialmedia.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    public FriendService(FriendRepository friendRepository, UserRepository userRepository) {
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
    }

    /**
     * Gửi lời mời kết bạn
     */
    @Transactional
    public Map<String, Object> sendFriendRequest(Integer senderId, Integer targetId) {
        if (senderId.equals(targetId)) {
            return Map.of("success", false, "message", "Không thể gửi lời mời kết bạn cho chính mình");
        }

        // Kiểm tra xem đã có friendship chưa
        Optional<Friend> existing = friendRepository.findFriendship(senderId, targetId);
        if (existing.isPresent()) {
            Friend f = existing.get();
            if (f.getStatus() == 0) {
                return Map.of("success", false, "message", "Đã có lời mời kết bạn đang chờ");
            } else if (f.getStatus() == 1) {
                return Map.of("success", false, "message", "Đã là bạn bè");
            } else if (f.getStatus() == 2) {
                return Map.of("success", false, "message", "Không thể gửi lời mời kết bạn");
            }
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        Friend friendRequest = new Friend();
        friendRequest.setUser(sender);
        friendRequest.setFriendUser(target);
        friendRequest.setStatus(0); // PENDING

        friendRepository.save(friendRequest);

        return Map.of("success", true, "message", "Đã gửi lời mời kết bạn");
    }

    /**
     * Chấp nhận lời mời kết bạn
     */
    @Transactional
    public Map<String, Object> acceptFriendRequest(Integer currentUserId, Integer requestFromUserId) {
        Optional<Friend> opt = friendRepository.findFriendship(requestFromUserId, currentUserId);
        if (opt.isEmpty()) {
            return Map.of("success", false, "message", "Không tìm thấy lời mời kết bạn");
        }

        Friend friendship = opt.get();
        // Phải là người nhận lời mời (friendUser)
        if (!friendship.getFriendUser().getUserId().equals(currentUserId)) {
            return Map.of("success", false, "message", "Bạn không có quyền chấp nhận lời mời này");
        }

        if (friendship.getStatus() != 0) {
            return Map.of("success", false, "message", "Lời mời không ở trạng thái chờ");
        }

        friendship.setStatus(1); // ACCEPTED
        friendRepository.save(friendship);

        return Map.of("success", true, "message", "Đã chấp nhận lời mời kết bạn");
    }

    /**
     * Từ chối lời mời kết bạn
     */
    @Transactional
    public Map<String, Object> rejectFriendRequest(Integer currentUserId, Integer requestFromUserId) {
        Optional<Friend> opt = friendRepository.findFriendship(requestFromUserId, currentUserId);
        if (opt.isEmpty()) {
            return Map.of("success", false, "message", "Không tìm thấy lời mời kết bạn");
        }

        Friend friendship = opt.get();
        // Phải là người nhận lời mời
        if (!friendship.getFriendUser().getUserId().equals(currentUserId)) {
            return Map.of("success", false, "message", "Bạn không có quyền từ chối lời mời này");
        }

        friendRepository.delete(friendship);

        return Map.of("success", true, "message", "Đã từ chối lời mời kết bạn");
    }

    /**
     * Xóa bạn bè hoặc hủy lời mời kết bạn
     * Nếu status = 1 (accepted): xóa bạn bè
     * Nếu status = 0 (pending): hủy lời mời
     */
    @Transactional
    public Map<String, Object> removeFriend(Integer currentUserId, Integer friendUserId) {
        Optional<Friend> opt = friendRepository.findFriendship(currentUserId, friendUserId);
        if (opt.isEmpty()) {
            return Map.of("success", false, "message", "Không tìm thấy mối quan hệ bạn bè");
        }

        Friend friendship = opt.get();
        
        // Có thể xóa nếu: là bạn bè (status=1) hoặc là người gửi lời mời (status=0)
        if (friendship.getStatus() == 1) {
            // Là bạn bè - xóa luôn
            friendRepository.delete(friendship);
            return Map.of("success", true, "message", "Đã xóa bạn bè");
        } else if (friendship.getStatus() == 0) {
            // Là lời mời chờ - chỉ người gửi mới được hủy
            if (friendship.getUser().getUserId().equals(currentUserId)) {
                // Current user là người gửi - có thể hủy
                friendRepository.delete(friendship);
                return Map.of("success", true, "message", "Đã hủy lời mời kết bạn");
            } else {
                return Map.of("success", false, "message", "Bạn không có quyền hủy lời mời này");
            }
        } else {
            return Map.of("success", false, "message", "Không thể xóa mối quan hệ này");
        }
    }

    /**
     * Block một user
     */
    @Transactional
    public Map<String, Object> blockUser(Integer currentUserId, Integer targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return Map.of("success", false, "message", "Không thể block chính mình");
        }

        Optional<Friend> opt = friendRepository.findFriendship(currentUserId, targetUserId);
        Friend friendship;

        if (opt.isPresent()) {
            friendship = opt.get();
            // Phải là người chủ động block
            if (!friendship.getUser().getUserId().equals(currentUserId)) {
                friendRepository.delete(friendship);
                
                // Tạo record mới với currentUser là user
                User currentUser = userRepository.findById(currentUserId).orElseThrow();
                User targetUser = userRepository.findById(targetUserId).orElseThrow();
                
                friendship = new Friend();
                friendship.setUser(currentUser);
                friendship.setFriendUser(targetUser);
            }
            friendship.setStatus(2); // BLOCKED
        } else {
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            friendship = new Friend();
            friendship.setUser(currentUser);
            friendship.setFriendUser(targetUser);
            friendship.setStatus(2); // BLOCKED
        }

        friendRepository.save(friendship);

        return Map.of("success", true, "message", "Đã block user");
    }

    /**
     * Lấy danh sách lời mời kết bạn đang chờ
     */
    public List<Map<String, Object>> getPendingRequests(Integer userId) {
        List<Friend> requests = friendRepository.findPendingRequestsForUser(userId);
        return requests.stream().map(f -> {
            User sender = f.getUser();
            Integer mutualCount = friendRepository.countMutualFriends(userId, sender.getUserId());
            
            Map<String, Object> map = new HashMap<>();
            map.put("userId", sender.getUserId());
            map.put("username", sender.getUserName());
            map.put("displayName", sender.getUserName());
            map.put("avatarUrl", sender.getAvatarUrl());
            map.put("mutualFriends", mutualCount != null ? mutualCount : 0);
            map.put("createdAt", f.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Lấy danh sách bạn bè
     */
    public List<Map<String, Object>> getFriendsList(Integer userId) {
        List<Friend> friends = friendRepository.findAcceptedFriendsForUser(userId);
        return friends.stream().map(f -> {
            User friend = f.getUser().getUserId().equals(userId) ? f.getFriendUser() : f.getUser();
            
            Map<String, Object> map = new HashMap<>();
            map.put("userId", friend.getUserId());
            map.put("username", friend.getUserName());
            map.put("displayName", friend.getUserName());
            map.put("avatarUrl", friend.getAvatarUrl());
            map.put("createdAt", f.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Gợi ý kết bạn (random users, không phải bạn bè hiện tại)
     */
    public List<Map<String, Object>> getSuggestions(Integer userId, int limit) {
        List<Integer> friendIds = friendRepository.findFriendUserIds(userId);
        friendIds.add(userId); // Không gợi ý chính mình

        List<User> allUsers = userRepository.findAll();
        List<User> suggestions = allUsers.stream()
                .filter(u -> !friendIds.contains(u.getUserId()))
                .limit(limit)
                .collect(Collectors.toList());

        return suggestions.stream().map(u -> {
            Integer mutualCount = friendRepository.countMutualFriends(userId, u.getUserId());
            
            Map<String, Object> map = new HashMap<>();
            map.put("userId", u.getUserId());
            map.put("username", u.getUserName());
            map.put("displayName", u.getUserName());
            map.put("avatarUrl", u.getAvatarUrl());
            map.put("mutualFriends", mutualCount != null ? mutualCount : 0);
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Kiểm tra trạng thái bạn bè giữa 2 user
     * none, pending_sent, pending_received, friends, blocked
     */
    public String getFriendshipStatus(Integer currentUserId, Integer targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return "self";
        }

        Optional<Friend> opt = friendRepository.findFriendship(currentUserId, targetUserId);
        if (opt.isEmpty()) {
            return "none";
        }

        Friend friendship = opt.get();
        if (friendship.getStatus() == 1) {
            return "friends";
        } else if (friendship.getStatus() == 2) {
            return friendship.getUser().getUserId().equals(currentUserId) ? "blocked" : "blocked_by";
        } else if (friendship.getStatus() == 0) {
            // Pending
            if (friendship.getUser().getUserId().equals(currentUserId)) {
                return "pending_sent";
            } else {
                return "pending_received";
            }
        }

        return "none";
    }

    /**
     * Check if viewer can see friend list of target user
     */
    public boolean canViewFriendList(Integer viewerId, Integer targetUserId) {
        // Chủ nhân luôn thấy friend list của mình
        if (viewerId != null && viewerId.equals(targetUserId)) {
            return true;
        }

        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) return false;

        String privacy = targetUser.getPrivacyFriendList();
        if (privacy == null) privacy = "ONLY_ME"; // default

        if ("EVERYONE".equals(privacy)) {
            return true;
        }

        if ("ONLY_ME".equals(privacy)) {
            // Chỉ chủ nhân
            return viewerId != null && viewerId.equals(targetUserId);
        }

        if ("FRIENDS".equals(privacy)) {
            // Chỉ bạn bè có thể xem
            if (viewerId == null) return false;
            // Check if viewerId is friend of targetUserId
            Optional<Friend> friendship = friendRepository.findFriendship(viewerId, targetUserId);
            return friendship.isPresent() && friendship.get().getStatus() == 1;
        }

        return false;
    }
}
