package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.socialmedia.backend.repository.*;

import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FriendRepository friendRepository;

    public UserService(UserRepository userRepository, PostRepository postRepository, FriendRepository friendRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.friendRepository = friendRepository;
    }

    @Transactional(readOnly = true)
    public User getMyProfile(Integer userId) {
        return userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    }

    @Transactional
    public User updateMyProfile(Integer userId, Map<String, String> body) {
        User u = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        // update: userName, email, avatarUrl, bio, phone
        String userName = body.get("userName");
        if (userName != null) {
            userName = userName.trim();
            if (userName.isBlank()) {
                throw new RuntimeException("USER_NAME_REQUIRED");
            }
            // validate format: letters, numbers, underscore, dot, hyphen; length 3-50
            if (!userName.matches("^[a-zA-Z0-9_.-]{3,50}$")) {
                throw new RuntimeException("INVALID_USER_NAME");
            }
            // check trùng (trừ chính mình)
            if (userRepository.existsByUserNameAndUserIdNot(userName, userId)) {
                throw new RuntimeException("USER_NAME_EXISTS");
            }
            u.setUserName(userName);
        }

        String email = body.get("email");
        if (email != null) {
            email = email.trim();
            if (email.isBlank()) {
                throw new RuntimeException("EMAIL_REQUIRED");
            }
            // basic email pattern
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new RuntimeException("INVALID_EMAIL");
            }
            if (userRepository.existsByEmailAndUserIdNot(email, userId)) {
                throw new RuntimeException("EMAIL_EXISTS");
            }
            u.setEmail(email);
        }

        String avatarUrl = body.get("avatarUrl");
        if (avatarUrl != null) {
            avatarUrl = avatarUrl.trim();
            u.setAvatarUrl(avatarUrl.isBlank() ? null : avatarUrl);
        }

        String phone = body.get("phone");
        if (phone != null) {
            phone = phone.trim();
            // allow digits, +, spaces, hyphen, parentheses, length 3-50
            if (!phone.isBlank() && !phone.matches("^[+0-9()\\-\\s]{3,50}$") && phone.length() > 10  ) {
                throw new RuntimeException("INVALID_PHONE");
            }
            u.setPhone(phone.isBlank() ? null : phone);
        }

        String coverUrl = body.get("coverUrl");
        if (coverUrl != null) {
            coverUrl = coverUrl.trim();
            u.setCoverUrl(coverUrl.isBlank() ? null : coverUrl);
        }

        String bio = body.get("bio");
        if (bio != null) {
            bio = bio.trim();
            u.setBio(bio.isBlank() ? null : bio);
        }

        String birthday = body.get("birthday");
        if (birthday != null) {
            birthday = birthday.trim();
            u.setBirthday(birthday.isBlank() ? null : birthday);
        }

        String location = body.get("location");
        if (location != null) {
            location = location.trim();
            u.setLocation(location.isBlank() ? null : location);
        }

        String relationship = body.get("relationship");
        if (relationship != null) {
            relationship = relationship.trim();
            u.setRelationship(relationship.isBlank() ? null : relationship);
        }

        String edu = body.get("edu");
        if (edu != null) {
            edu = edu.trim();
            u.setEdu(edu.isBlank() ? null : edu);
        }

        return userRepository.save(u);
    }

    // Helper trả JSON map (đỡ lộ passWord)
    public Map<String, Object> toProfileResponse(User u) {
        Map<String, Object> res = new java.util.HashMap<>();
        res.put("userId", u.getUserId());
        res.put("userName", u.getUserName());
        res.put("email", u.getEmail());
        res.put("avatarUrl", u.getAvatarUrl()); // null OK
        res.put("coverUrl", u.getCoverUrl());     // null OK
        res.put("bio", u.getBio());             // null OK
        res.put("birthday", u.getBirthday());   // null OK
        res.put("location", u.getLocation());   // null OK
        res.put("relationship", u.getRelationship()); // null OK
        res.put("edu", u.getEdu());             // null OK
        res.put("status", u.getStatus());       // null OK

        // posts count
        long postCount = postRepository.countByUserId(u.getUserId());
        res.put("postCount", postCount);
        res.put("phone", u.getPhone()); // null OK

        // friends count - now properly retrieved
        Integer friendCount = friendRepository.countFriends(u.getUserId());
        res.put("friendCount", friendCount != null ? friendCount : 0);
        res.put("communityCount", 0);

        res.put("createdAt", u.getCreatedAt()); // null OK
        res.put("role", (u.getRole() != null ? u.getRole().getRoleName() : null)); // null OK
        return res;
    }

    @Transactional(readOnly = true)
    public User findById(Integer userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}
