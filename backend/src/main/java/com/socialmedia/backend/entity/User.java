package com.socialmedia.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "[user]") // nếu DB là [user]
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_name", nullable = false, unique = true, length = 50)
    private String userName;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "pass_word", nullable = false, length = 255)
    private String passWord;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    @Column(name = "bio", length = 2000)
    private String bio;

    @Column(name = "birthday", length = 50)
    private String birthday;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "relationship", length = 50)
    private String relationship;

    @Column(name = "edu", length = 255)
    private String edu;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Privacy Settings
    @Column(name = "privacy_posts", length = 50)
    private String privacyPosts = "FRIENDS"; // EVERYONE, FRIENDS, ONLY_ME

    @Column(name = "privacy_friend_requests", length = 50)
    private String privacyFriendRequests = "EVERYONE"; // EVERYONE, FRIENDS_OF_FRIENDS, NO_ONE

    @Column(name = "privacy_friend_list", length = 50)
    private String privacyFriendList = "ONLY_ME"; // EVERYONE, FRIENDS, ONLY_ME

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassWord() { return passWord; }
    public void setPassWord(String passWord) { this.passWord = passWord; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getEdu() { return edu; }
    public void setEdu(String edu) { this.edu = edu; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPrivacyPosts() { return privacyPosts; }
    public void setPrivacyPosts(String privacyPosts) { this.privacyPosts = privacyPosts; }

    public String getPrivacyFriendRequests() { return privacyFriendRequests; }
    public void setPrivacyFriendRequests(String privacyFriendRequests) { this.privacyFriendRequests = privacyFriendRequests; }

    public String getPrivacyFriendList() { return privacyFriendList; }
    public void setPrivacyFriendList(String privacyFriendList) { this.privacyFriendList = privacyFriendList; }
}
