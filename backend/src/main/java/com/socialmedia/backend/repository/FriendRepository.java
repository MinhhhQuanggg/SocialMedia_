package com.socialmedia.backend.repository;

import com.socialmedia.backend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Integer> {

    /**
     * Tìm mối quan hệ bạn bè theo 2 user (bất kể ai là người gửi)
     */
    @Query("SELECT f FROM Friend f WHERE " +
           "(f.user.userId = :userId AND f.friendUser.userId = :friendId) OR " +
           "(f.user.userId = :friendId AND f.friendUser.userId = :userId)")
    Optional<Friend> findFriendship(@Param("userId") Integer userId, 
                                    @Param("friendId") Integer friendId);

    /**
     * Lấy danh sách lời mời kết bạn đang chờ (PENDING) mà user nhận được
     */
    @Query("SELECT f FROM Friend f WHERE f.friendUser.userId = :userId AND f.status = 0")
    List<Friend> findPendingRequestsForUser(@Param("userId") Integer userId);

    /**
     * Lấy danh sách bạn bè đã chấp nhận (ACCEPTED) của user
     */
    @Query("SELECT f FROM Friend f WHERE " +
           "(f.user.userId = :userId OR f.friendUser.userId = :userId) AND f.status = 1")
    List<Friend> findAcceptedFriendsForUser(@Param("userId") Integer userId);

    /**
     * Đếm số lượng bạn bè của user
     */
    @Query("SELECT COUNT(f) FROM Friend f WHERE " +
           "(f.user.userId = :userId OR f.friendUser.userId = :userId) AND f.status = 1")
    Integer countFriends(@Param("userId") Integer userId);

    /**
     * Lấy danh sách user ID của tất cả bạn bè (ACCEPTED)
     */
    @Query("SELECT CASE WHEN f.user.userId = :userId THEN f.friendUser.userId ELSE f.user.userId END " +
           "FROM Friend f WHERE (f.user.userId = :userId OR f.friendUser.userId = :userId) AND f.status = 1")
    List<Integer> findFriendUserIds(@Param("userId") Integer userId);

    /**
     * Kiểm tra xem có friendship nào giữa 2 user không (bất kể status)
     */
    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE " +
           "(f.user.userId = :userId AND f.friendUser.userId = :friendId) OR " +
           "(f.user.userId = :friendId AND f.friendUser.userId = :userId)")
    boolean existsFriendship(@Param("userId") Integer userId, 
                             @Param("friendId") Integer friendId);

    /**
     * Lấy danh sách các user mà currentUser đã block
     */
    @Query("SELECT f FROM Friend f WHERE f.user.userId = :userId AND f.status = 2")
    List<Friend> findBlockedUsers(@Param("userId") Integer userId);

    /**
     * Đếm số bạn chung giữa 2 user
     */
    @Query("SELECT COUNT(DISTINCT CASE WHEN f1.user.userId = :userId1 THEN f1.friendUser.userId ELSE f1.user.userId END) " +
           "FROM Friend f1, Friend f2 WHERE " +
           "f1.status = 1 AND f2.status = 1 AND " +
           "((f1.user.userId = :userId1 OR f1.friendUser.userId = :userId1) AND " +
           " (f2.user.userId = :userId2 OR f2.friendUser.userId = :userId2) AND " +
           " (CASE WHEN f1.user.userId = :userId1 THEN f1.friendUser.userId ELSE f1.user.userId END) = " +
           " (CASE WHEN f2.user.userId = :userId2 THEN f2.friendUser.userId ELSE f2.user.userId END))")
    Integer countMutualFriends(@Param("userId1") Integer userId1, 
                               @Param("userId2") Integer userId2);
}
