package com.socialmedia.backend.repository;

import com.socialmedia.backend.entity.Group;
import com.socialmedia.backend.entity.GroupMember;
import com.socialmedia.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Integer> {
    List<GroupMember> findByUser(User user);
    List<GroupMember> findByGroup(Group group);
    Optional<GroupMember> findByGroupAndUser(Group group, User user);
    void deleteByGroupAndUser(Group group, User user);
}
