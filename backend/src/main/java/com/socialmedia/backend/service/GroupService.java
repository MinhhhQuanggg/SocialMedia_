package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.Group;
import com.socialmedia.backend.entity.GroupMember;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.GroupMemberRepository;
import com.socialmedia.backend.repository.GroupRepository;
import com.socialmedia.backend.repository.UserRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                       GroupMemberRepository groupMemberRepository,
                       UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new group
     */
    public Group createGroup(String groupName, String description, Integer createdByUserId, String avatarUrl) {
        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = new Group();
        group.setGroupName(groupName);
        group.setDescription(description);
        group.setAvatarUrl(avatarUrl);
        group.setCreatedBy(createdBy);

        Group savedGroup = groupRepository.save(group);

        // Add creator as admin
        GroupMember admin = new GroupMember();
        admin.setGroup(savedGroup);
        admin.setUser(createdBy);
        admin.setRole(GroupMember.GroupRole.ADMIN);
        groupMemberRepository.save(admin);

        return initializeGroup(savedGroup);
    }

    /**
     * Get all groups of a user
     */
    public List<Group> getUserGroups(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return groupMemberRepository.findByUser(user)
                .stream()
                .map(GroupMember::getGroup)
                .map(this::initializeGroup)
                .collect(Collectors.toList());
    }

    /**
     * Get group by ID
     */
    public Group getGroup(Integer groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return initializeGroup(group);
    }

    /**
     * Initialize lazy-loaded properties
     */
    private Group initializeGroup(Group group) {
        Hibernate.initialize(group.getCreatedBy());
        if (group.getCreatedBy() != null) {
            Hibernate.initialize(group.getCreatedBy().getRole());
        }
        Hibernate.initialize(group.getMembers());
        Hibernate.initialize(group.getMessages());
        return group;
    }

    /**
     * Add member to group
     */
    public GroupMember addMember(Integer groupId, Integer userId) {
        Group group = getGroup(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        groupMemberRepository.findByGroupAndUser(group, user)
                .ifPresent(m -> { throw new RuntimeException("User is already a member of this group"); });

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(GroupMember.GroupRole.MEMBER);
        return groupMemberRepository.save(member);
    }

    /**
     * Remove member from group
     */
    public void removeMember(Integer groupId, Integer userId) {
        Group group = getGroup(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        groupMemberRepository.deleteByGroupAndUser(group, user);
    }

    /**
     * Get group members
     */
    public List<GroupMember> getGroupMembers(Integer groupId) {
        return groupMemberRepository.findByGroup(getGroup(groupId));
    }

    /**
     * Change member role
     */
    public GroupMember changeMemberRole(Integer groupId, Integer userId, GroupMember.GroupRole newRole) {
        Group group = getGroup(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        member.setRole(newRole);
        return groupMemberRepository.save(member);
    }

    /**
     * Update group info
     */
    public Group updateGroup(Integer groupId, String groupName, String description, String avatarUrl) {
        Group group = getGroup(groupId);
        if (groupName != null) group.setGroupName(groupName);
        if (description != null) group.setDescription(description);
        if (avatarUrl != null) group.setAvatarUrl(avatarUrl);
        return groupRepository.save(group);
    }

    /**
     * Delete group
     */
    public void deleteGroup(Integer groupId) {
        groupRepository.delete(getGroup(groupId));
    }

    /**
     * Increment unread count for all members except sender
     */
    public void incrementUnreadCount(Integer groupId, Integer senderId) {
        Group group = getGroup(groupId);
        group.getMembers().stream()
                .filter(m -> !m.getUser().getUserId().equals(senderId))
                .forEach(m -> {
                    m.setUnreadCount(m.getUnreadCount() + 1);
                    groupMemberRepository.save(m);
                });
    }

    /**
     * Reset unread count for user
     */
    public void resetUnreadCount(Integer groupId, Integer userId) {
        Group group = getGroup(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        groupMemberRepository.findByGroupAndUser(group, user).ifPresent(m -> {
            m.setUnreadCount(0);
            groupMemberRepository.save(m);
        });
    }

    /**
     * Get unread count for user in group
     */
    public Integer getUnreadCount(Integer groupId, Integer userId) {
        Group group = getGroup(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return groupMemberRepository.findByGroupAndUser(group, user)
                .map(GroupMember::getUnreadCount)
                .orElse(0);
    }

    /**
     * Broadcast unread counts to all group members
     */
    public void broadcastGroupUnreadCounts(Integer groupId, com.socialmedia.backend.websocket.ChatWebSocketHandler handler) {
        try {
            Group group = getGroup(groupId);
            group.getMembers().forEach(member -> {
                Map<String, Object> event = new HashMap<>();
                event.put("type", "GROUP_UNREAD_UPDATE");
                event.put("groupId", groupId);
                event.put("unreadCount", member.getUnreadCount());
                handler.sendToUser(member.getUser().getUserId(), event);
            });
        } catch (Exception e) {
            // Silently fail
        }
    }

    /**
     * Check if user is member of group
     */
    public boolean isMember(Integer groupId, Integer userId) {
        Group group = getGroup(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return groupMemberRepository.findByGroupAndUser(group, user).isPresent();
    }

    /**
     * Check if user is admin of group
     */
    public boolean isAdmin(Integer groupId, Integer userId) {
        Group group = getGroup(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<GroupMember> member = groupMemberRepository.findByGroupAndUser(group, user);
        return member.isPresent() && member.get().getRole() == GroupMember.GroupRole.ADMIN;
    }
}
