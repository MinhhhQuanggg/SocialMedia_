package com.socialmedia.backend.repository;

import com.socialmedia.backend.entity.Reaction;
import com.socialmedia.backend.entity.Post;
import com.socialmedia.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Integer> {


    boolean existsByPostAndUser(Post post, User user);

    Optional<Reaction> findByPostAndUser(Post post, User user);

    long countByPost(Post post);

    long countByPostAndType(Post post, String type);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.post.postId = :postId")
    void deleteByPostId(Integer postId);

    @Query("SELECT COUNT(r) FROM Reaction r")
    long countAll();
}
