package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.Message;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.MessageRepository;
import com.socialmedia.backend.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final Path mediaStoragePath;
    private final Path fileStoragePath;
    private final com.socialmedia.backend.websocket.ChatWebSocketHandler chatWebSocketHandler;

    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          @Lazy com.socialmedia.backend.websocket.ChatWebSocketHandler chatWebSocketHandler) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.chatWebSocketHandler = chatWebSocketHandler;
        // Store media (images/videos) in project root: uploads/media
        this.mediaStoragePath = Paths.get(System.getProperty("user.dir"), "uploads", "media");
        // Store other files in project root: uploads/files
        this.fileStoragePath = Paths.get(System.getProperty("user.dir"), "uploads", "files");
        
        // Create storage directories
        try {
            Files.createDirectories(mediaStoragePath);
            Files.createDirectories(fileStoragePath);
            System.out.println("Media storage: " + mediaStoragePath.toAbsolutePath());
            System.out.println("File storage: " + fileStoragePath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directories", e);
        }
    }

    @Transactional
    public Message send(Integer fromUserId, Integer toUserId, Map<String, String> body) {
        if (fromUserId == null) throw new RuntimeException("UNAUTHORIZED");
        if (toUserId == null) throw new RuntimeException("RECEIVER_REQUIRED");
        if (fromUserId.equals(toUserId)) throw new RuntimeException("CANNOT_MESSAGE_YOURSELF");

        User sender = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        User receiver = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("RECEIVER_NOT_FOUND"));

        String content = body.get("content");
        if (content != null) {
            content = content.trim();
            content = content.isBlank() ? null : content;
        }

        String imageUrl = body.get("imageUrl");
        if (imageUrl != null) {
            imageUrl = imageUrl.trim();
            imageUrl = imageUrl.isBlank() ? null : imageUrl;
        }

        String videoUrl = body.get("videoUrl");
        if (videoUrl != null) {
            videoUrl = videoUrl.trim();
            videoUrl = videoUrl.isBlank() ? null : videoUrl;
        }

        if (content == null && imageUrl == null && videoUrl == null) {
            throw new RuntimeException("MESSAGE_EMPTY");
        }

        Message m = new Message();
        m.setSender(sender);
        m.setReceiver(receiver);
        m.setContent(content);
        m.setImageUrl(imageUrl);
        m.setVideoUrl(videoUrl);
        m.setStatus(false); // unread

        Message saved = messageRepository.save(m);
        // Notify receiver about new unread count
        notifyUnreadCount(receiver.getUserId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Message> getConversation(Integer me, Integer otherId) {
        if (me == null) throw new RuntimeException("UNAUTHORIZED");
        return messageRepository.findConversation(me, otherId);
    }

    @Transactional
    public Message markRead(Integer me, Integer messageId) {
        Message m = messageRepository.findByIdWithUsers(messageId)
                .orElseThrow(() -> new RuntimeException("MESSAGE_NOT_FOUND"));

        // chỉ receiver mới được mark read
        if (m.getReceiver() == null || !m.getReceiver().getUserId().equals(me)) {
            throw new RuntimeException("FORBIDDEN");
        }

        m.setStatus(true);
        m.setReadAt(LocalDateTime.now());
        Message saved = messageRepository.save(m);
        // Notify receiver (me) about updated unread count
        notifyUnreadCount(me);
        return saved;
    }

    @Transactional(readOnly = true)
    public long countUnread(Integer me) {
        if (me == null) throw new RuntimeException("UNAUTHORIZED");
        return messageRepository.countUnread(me);
    }

    @Transactional
    public int markAllReadFromUser(Integer me, Integer otherUserId) {
        if (me == null) throw new RuntimeException("UNAUTHORIZED");
        
        System.out.println("markAllReadFromUser called: me=" + me + ", otherUserId=" + otherUserId);
        
        // Lấy tất cả tin nhắn chưa đọc từ otherUserId gửi cho me
        List<Message> unreadMessages = messageRepository.findUnreadFromSender(me, otherUserId);
        
        System.out.println("Found " + unreadMessages.size() + " unread messages from user " + otherUserId);
        
        LocalDateTime now = LocalDateTime.now();
        for (Message m : unreadMessages) {
            System.out.println("Marking message " + m.getMessageId() + " as read");
            m.setStatus(true);
            m.setReadAt(now);
        }
        
        if (!unreadMessages.isEmpty()) {
            messageRepository.saveAll(unreadMessages);
            System.out.println("Saved " + unreadMessages.size() + " messages");
            // Notify receiver (me) about updated unread count
            notifyUnreadCount(me);
        }

        return unreadMessages.size();
    }

    @Transactional
    public Message sendMedia(Integer fromUserId, Integer toUserId, MultipartFile file, String mediaType) throws IOException {
        if (fromUserId == null) throw new RuntimeException("UNAUTHORIZED");
        if (toUserId == null) throw new RuntimeException("RECEIVER_REQUIRED");
        if (fromUserId.equals(toUserId)) throw new RuntimeException("CANNOT_MESSAGE_YOURSELF");
        if (file == null || file.isEmpty()) throw new RuntimeException("FILE_REQUIRED");

        User sender = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        User receiver = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("RECEIVER_NOT_FOUND"));

        // Validate file size (50MB max)
        long maxFileSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("FILE_TOO_LARGE");
        }

        // Determine file type by MIME type
        String contentType = file.getContentType();
        List<String> allowedImages = Arrays.asList("image/jpeg", "image/png", "image/gif");
        List<String> allowedVideos = Arrays.asList("video/mp4", "video/webm");
        
        boolean isImage = allowedImages.contains(contentType);
        boolean isVideo = allowedVideos.contains(contentType);
        boolean isFile = !isImage && !isVideo; // Accept any other file type

        // Generate unique filename
        String fileName = generateMediaFileName(file.getOriginalFilename());
        
        // Choose storage path based on file type
        Path storagePath = (isImage || isVideo) ? mediaStoragePath : fileStoragePath;
        Path filePath = storagePath.resolve(fileName);
        
        // Save file to disk
        Files.write(filePath, file.getBytes());
        System.out.println("File saved: " + filePath.toAbsolutePath());

        // Create message
        Message m = new Message();
        m.setSender(sender);
        m.setReceiver(receiver);
        m.setContent(null);
        m.setStatus(false); // unread

        if (isImage) {
            m.setImageUrl("/uploads/media/" + fileName);
        } else if (isVideo) {
            m.setVideoUrl("/uploads/media/" + fileName);
        } else {
            // For other file types, store in uploads/files
            m.setContent("[FILE] " + file.getOriginalFilename());
            m.setImageUrl("/uploads/files/" + fileName); // Store URL for download
        }

        return messageRepository.save(m);
    }

    /**
     * Generate unique media file name
     */
    private String generateMediaFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        }
        return UUID.randomUUID().toString() + extension;
    }

    // Map response giống PostService.toPostResponse
    public Map<String, Object> toMessageResponse(Message m) {
        Map<String, Object> res = new HashMap<>();
        res.put("messageId", m.getMessageId());
        res.put("content", m.getContent());
        res.put("imageUrl", m.getImageUrl());   // có thể null -> OK
        res.put("videoUrl", m.getVideoUrl());
        res.put("sentAt", m.getSentAt());
        res.put("readAt", m.getReadAt());
        res.put("status", m.getStatus());

        // sender
        if (m.getSender() != null) {
            Map<String, Object> sender = new HashMap<>();
            sender.put("userId", m.getSender().getUserId());
            sender.put("userName", m.getSender().getUserName());
            sender.put("avatarUrl", m.getSender().getAvatarUrl()); // null vẫn OK
            res.put("sender", sender);
        } else {
            res.put("sender", null);
        }

        // receiver
        if (m.getReceiver() != null) {
            Map<String, Object> receiver = new HashMap<>();
            receiver.put("userId", m.getReceiver().getUserId());
            receiver.put("userName", m.getReceiver().getUserName());
            receiver.put("avatarUrl", m.getReceiver().getAvatarUrl()); // null vẫn OK
            res.put("receiver", receiver);
        } else {
            res.put("receiver", null);
        }

        // group (for group messages)
        if (m.getGroup() != null) {
            Map<String, Object> group = new HashMap<>();
            group.put("groupId", m.getGroup().getGroupId());
            group.put("groupName", m.getGroup().getGroupName());
            group.put("avatarUrl", m.getGroup().getAvatarUrl());
            res.put("group", group);
        } else {
            res.put("group", null);
        }

        return res;
    }

    /**
     * Save message (used by WebSocket for group messages)
     */
    @Transactional
    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

    private void notifyUnreadCount(Integer userId) {
        try {
            long unread = countUnread(userId);
            Map<String, Object> event = new HashMap<>();
            event.put("type", "UNREAD_COUNT");
            event.put("unread", unread);
            chatWebSocketHandler.sendToUser(userId, event);
        } catch (Exception ignored) {}
    }
}
