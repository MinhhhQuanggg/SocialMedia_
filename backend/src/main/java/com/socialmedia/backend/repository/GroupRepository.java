package com.socialmedia.backend.repository;

import com.socialmedia.backend.entity.Group;
import com.socialmedia.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {
    List<Group> findByCreatedBy(User createdBy);
}
