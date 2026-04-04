package com.socialmedia.backend.repository;

import com.socialmedia.backend.entity.Notification;
import com.socialmedia.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // GET list notifications (KHÔNG @Modifying)
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // UPDATE read (CÓ @Modifying, KHÔNG Pageable/Sort/Limit)
    @Modifying
    @Transactional
    @Query("""
        update Notification n
        set n.isRead = true
        where n.notificationId = :id and n.user.userId = :userId
    """)
    int markAsRead(@Param("id") Integer id, @Param("userId") Integer userId);

    //đếm notification
    @Query("""
    SELECT COUNT(n)
    FROM Notification n
    WHERE n.user = :user
      AND (n.isRead = false OR n.isRead IS NULL)
    """)
    long countUnreadByUser(@Param("user") User user);

    // Xóa notifications theo post_id
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.post.postId = :postId")
    void deleteByPostId(@Param("postId") Integer postId);

}
