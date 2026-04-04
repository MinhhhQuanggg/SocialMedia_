package com.socialmedia.backend.repository;

import com.socialmedia.backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Integer> {

    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.user u
        WHERE p.postId = :postId
    """)
    Optional<Post> findByIdWithUser(@Param("postId") Integer postId);

    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.user u
        WHERE u.userId = :userId
        ORDER BY p.createdAt DESC
    """)
    List<Post> findMyPosts(@Param("userId") Integer userId);

    // Feed đơn giản: lấy tất cả bài (sau bạn có friends/follow thì lọc tiếp)
    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.user u
        ORDER BY p.createdAt DESC
    """)
    List<Post> findFeed();

    // PostRepository.java
    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.user u
        WHERE (:q IS NULL OR :q = '' OR LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY p.createdAt DESC
    """)
    List<Post> searchPosts(@Param("q") String q);

    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.user u
        ORDER BY p.createdAt DESC
    """)
    List<Post> adminListAll();

    @Query("SELECT COUNT(p) FROM Post p")
    long countAll();

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.userId = :userId")
    long countByUserId(@Param("userId") Integer userId);

}
