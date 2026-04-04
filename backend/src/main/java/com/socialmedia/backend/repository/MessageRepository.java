package com.socialmedia.backend.repository;

import com.socialmedia.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    // Lấy hội thoại 2 chiều (A<->B), join fetch để khỏi Lazy khi trả JSON map
    @Query("""
        SELECT m FROM Message m
        LEFT JOIN FETCH m.sender s
        LEFT JOIN FETCH m.receiver r
        WHERE (s.userId = :a AND r.userId = :b)
           OR (s.userId = :b AND r.userId = :a)
        ORDER BY m.sentAt ASC
    """)
    List<Message> findConversation(@Param("a") Integer a, @Param("b") Integer b);

    @Query("""
        SELECT m FROM Message m
        LEFT JOIN FETCH m.sender s
        LEFT JOIN FETCH m.receiver r
        WHERE m.messageId = :id
    """)
    Optional<Message> findByIdWithUsers(@Param("id") Integer id);

    // Đếm tin chưa đọc của 1 user (receiver)
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.receiver.userId = :userId
          AND (m.status = false OR m.status IS NULL)
    """)
    long countUnread(@Param("userId") Integer userId);

    // Lấy tất cả tin nhắn chưa đọc từ một sender
    @Query("""
        SELECT m FROM Message m
        WHERE m.receiver.userId = :receiverId
          AND m.sender.userId = :senderId
          AND (m.status = false OR m.status IS NULL)
    """)
    List<Message> findUnreadFromSender(@Param("receiverId") Integer receiverId, @Param("senderId") Integer senderId);
}
