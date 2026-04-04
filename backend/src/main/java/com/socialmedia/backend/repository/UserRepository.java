package com.socialmedia.backend.repository;

import com.socialmedia.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
    Optional<User> findByUserName(String userName);

    Optional<User> findByEmailOrUserName(String email, String userName);

    // ===== FIX LAZY ROLE =====

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
        FROM User u
        WHERE u.userName = :userName AND u.userId <> :userId
    """)
    boolean existsByUserNameAndUserIdNot(@Param("userName") String userName,
                                         @Param("userId") Integer userId);

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
        FROM User u
        WHERE u.email = :email AND u.userId <> :userId
    """)
    boolean existsByEmailAndUserIdNot(@Param("email") String email,
                                     @Param("userId") Integer userId);

    // Login: lấy user kèm role
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role
        WHERE u.email = :login OR u.userName = :login
    """)
    Optional<User> findForLoginWithRole(@Param("login") String login);

    // Profile: lấy user theo id kèm role
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role
        WHERE u.userId = :id
    """)
    Optional<User> findByIdWithRole(@Param("id") Integer id);

        // UserRepository.java
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role r
        WHERE (:q IS NULL OR :q = '' 
            OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
        )
        ORDER BY u.userName ASC
    """)
    List<User> searchUsers(@Param("q") String q);

    // Admin: lấy danh sách user kèm role để khỏi lazy
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role r
        ORDER BY u.userId DESC
    """)
    List<User> findAllWithRole();

    // Admin: search theo username/email
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role r
        WHERE (:q IS NULL OR :q = '' OR
               LOWER(u.userName) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY u.userId DESC
    """)
    List<User> searchWithRole(@Param("q") String q);
}
