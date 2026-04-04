package com.socialmedia.backend.repository;

import com.socialmedia.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    @Query("""
        SELECT c FROM Comment c
        LEFT JOIN FETCH c.user u
        WHERE c.post.postId = :postId
        ORDER BY c.createdAt ASC
    """)
    List<Comment> findByPostId(@Param("postId") Integer postId);

    @Query("""
        SELECT c FROM Comment c
        LEFT JOIN FETCH c.user u
        WHERE c.commentId = :id
    """)
    Optional<Comment> findByIdWithUser(@Param("id") Integer id);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.postId = :postId")
    long countByPostId(@Param("postId") Integer postId);

    @Query("""
        SELECT c FROM Comment c
        LEFT JOIN FETCH c.user u
        LEFT JOIN FETCH c.post p
        WHERE c.commentId = :id
    """)
    Optional<Comment> findByIdWithUserAndPost(@Param("id") Integer id);

    @Query("""
        SELECT c FROM Comment c
        LEFT JOIN FETCH c.user u
        WHERE c.post.postId = :postId
        ORDER BY c.createdAt DESC
    """)
    List<Comment> findByPostIdWithUser(@Param("postId") Integer postId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.postId = :postId")
    void deleteByPostId(Integer postId);

    @Query("SELECT COUNT(c) FROM Comment c")
    long countAll();
}
