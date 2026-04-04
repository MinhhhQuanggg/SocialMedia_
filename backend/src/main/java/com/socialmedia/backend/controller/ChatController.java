package com.socialmedia.backend.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.socialmedia.backend.entity.Group;
import com.socialmedia.backend.entity.GroupMember;
import com.socialmedia.backend.entity.Message;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.MessageRepository;
import com.socialmedia.backend.repository.UserRepository;
import com.socialmedia.backend.service.GroupService;
import com.socialmedia.backend.service.MessageService;
import com.socialmedia.backend.websocket.ChatWebSocketHandler;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final MessageService messageService;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final Path mediaStoragePath;
    private final Path fileStoragePath;

    public ChatController(MessageService messageService, 
                        MessageRepository messageRepository,
                        UserRepository userRepository,
                        GroupService groupService,
                        ChatWebSocketHandler chatWebSocketHandler) {
        this.messageService = messageService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
        this.chatWebSocketHandler = chatWebSocketHandler;
        // Store under project folder: uploads/media for images/videos
        this.mediaStoragePath = Paths.get(System.getProperty("user.dir"), "uploads", "media");
        // Store under project folder: uploads/files for documents
        this.fileStoragePath = Paths.get(System.getProperty("user.dir"), "uploads", "files");

        try {
            Files.createDirectories(this.mediaStoragePath);
            logger.info("Media storage directory ready: {}", mediaStoragePath.toAbsolutePath());
            Files.createDirectories(this.fileStoragePath);
            logger.info("File storage directory ready: {}", fileStoragePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create storage directories", e);
        }
    }

    private Integer requireUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("UNAUTHORIZED");
        }
        Object principal = a.getPrincipal();
        if (principal == null) throw new RuntimeException("UNAUTHORIZED");
        try {
            return Integer.valueOf(principal.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("UNAUTHORIZED");
        }
    }

    /**
     * Send media (image or video) to another user
     */
    @PostMapping("/send-media")
    public ResponseEntity<?> sendMedia(
            @RequestParam Integer toUserId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false, defaultValue = "image") String mediaType,
            HttpServletRequest request) {
        
        Path filePath = null;
        boolean fileWritten = false;

        try {
            Integer fromUserId = requireUserId();
            logger.info("Media send request: from={}, to={}, file={}, type={}, size={}MB", 
                fromUserId, toUserId, file.getOriginalFilename(), mediaType, file.getSize() / 1024 / 1024);
            
            // Validate inputs
            if (toUserId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "toUserId required"));
            }
            
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "file required"));
            }

            // File size validation (50MB max)
            long maxFileSize = 50 * 1024 * 1024;
            if (file.getSize() > maxFileSize) {
                logger.warn("File too large: {} bytes", file.getSize());
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(Map.of("error", "FILE_TOO_LARGE", "max", "50MB"));
            }

            // Validate file type
            String contentType = file.getContentType();
            logger.info("File MIME type: {}", contentType);
            
            List<String> allowedImages = Arrays.asList("image/jpeg", "image/png", "image/gif");
            List<String> allowedVideos = Arrays.asList("video/mp4", "video/webm");
            
            boolean isImage = allowedImages.contains(contentType);
            boolean isVideo = allowedVideos.contains(contentType);
            boolean isFile = !isImage && !isVideo; // Accept any other file type

            logger.info("File type classification: isImage={}, isVideo={}, isFile={}", isImage, isVideo, isFile);

            // Check users exist
            User sender = userRepository.findById(fromUserId)
                    .orElseThrow(() -> new RuntimeException("SENDER_NOT_FOUND"));
            User receiver = userRepository.findById(toUserId)
                    .orElseThrow(() -> new RuntimeException("RECEIVER_NOT_FOUND"));

            // Generate unique filename
            String fileName = generateMediaFileName(file.getOriginalFilename());
            
            // Determine storage path based on file type
            Path storagePath;
            String urlPath;
            if (isImage || isVideo) {
                storagePath = mediaStoragePath.resolve(fileName);
                urlPath = "/uploads/media/";
            } else {
                storagePath = fileStoragePath.resolve(fileName);
                urlPath = "/uploads/files/";
            }
            filePath = storagePath;

            // Save file to disk
            byte[] fileBytes = file.getBytes();
            Files.write(filePath, fileBytes);

            // Validate file was written correctly
            if (!Files.exists(filePath)) {
                throw new IOException("File not found after write: " + filePath);
            }
            long writtenSize = Files.size(filePath);
            if (writtenSize != fileBytes.length) {
                Files.deleteIfExists(filePath);
                throw new IOException("Written size mismatch. expected=" + fileBytes.length + " actual=" + writtenSize);
            }

            fileWritten = true;
            logger.info("File saved to: {} ({} bytes)", filePath.toAbsolutePath(), writtenSize);

            // Create message record in database
            Message message = new Message();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setContent(null);
            message.setStatus(false); // unread
            
            // Build full URL for media (use backend base URL from request)
            String backendBaseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String mediaUrl = backendBaseUrl + urlPath + fileName;
            
            if (isImage) {
                message.setImageUrl(mediaUrl);
            } else if (isVideo) {
                message.setVideoUrl(mediaUrl);
            } else {
                // For other file types, store as file with download link
                message.setContent("[FILE] " + file.getOriginalFilename());
                message.setImageUrl(mediaUrl); // Store URL for download
            }

            Message savedMessage = messageRepository.save(message);
            logger.info("Message saved to DB: id={}, imageUrl={}, videoUrl={}, content={}", 
                savedMessage.getMessageId(), savedMessage.getImageUrl(), savedMessage.getVideoUrl(), savedMessage.getContent());

            // Broadcast via WebSocket to both users
            Map<String, Object> wsEvent = new HashMap<>();
            wsEvent.put("type", "NEW_MESSAGE");
            wsEvent.put("data", messageService.toMessageResponse(savedMessage));
            chatWebSocketHandler.broadcastEvent(wsEvent);
            logger.info("Broadcast NEW_MESSAGE via WebSocket for media message id={}", savedMessage.getMessageId());

            // Return response
            Map<String, Object> response = new HashMap<>();
            response.put("messageId", savedMessage.getMessageId());
            
            Map<String, Object> senderMap = new HashMap<>();
            senderMap.put("userId", sender.getUserId());
            senderMap.put("userName", sender.getUserName());
            senderMap.put("avatarUrl", sender.getAvatarUrl());
            response.put("sender", senderMap);
            
            Map<String, Object> receiverMap = new HashMap<>();
            receiverMap.put("userId", receiver.getUserId());
            receiverMap.put("userName", receiver.getUserName());
            receiverMap.put("avatarUrl", receiver.getAvatarUrl());
            response.put("receiver", receiverMap);
            
            response.put("content", savedMessage.getContent());
            response.put("imageUrl", savedMessage.getImageUrl());
            response.put("videoUrl", savedMessage.getVideoUrl());
            response.put("sentAt", savedMessage.getSentAt());
            response.put("status", savedMessage.getStatus());
            
            logger.info("Send media successful, returning response");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("Runtime error in sendMedia: {}", e.getMessage());
            if ("UNAUTHORIZED".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "UNAUTHORIZED"));
            }
            if ("SENDER_NOT_FOUND".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "SENDER_NOT_FOUND"));
            }
            if ("RECEIVER_NOT_FOUND".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "RECEIVER_NOT_FOUND"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            logger.error("IO error in sendMedia", e);
            if (fileWritten && filePath != null) {
                try { Files.deleteIfExists(filePath); } catch (IOException ignore) {}
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "FILE_WRITE_ERROR", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error in sendMedia", e);
            if (fileWritten && filePath != null) {
                try { Files.deleteIfExists(filePath); } catch (IOException ignore) {}
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL_SERVER_ERROR", "message", e.getMessage()));
        }
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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        logger.error("RuntimeException in ChatController", ex);
        if ("UNAUTHORIZED".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "UNAUTHORIZED"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
    }

    /* ====================================
       GROUP CHAT ENDPOINTS
    ==================================== */

    /**
     * Create a new group
     */
    @PostMapping("/group/create")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> request) {
        try {
            Integer userId = requireUserId();
            String groupName = (String) request.get("groupName");
            String description = (String) request.get("description");
            String avatarUrl = (String) request.getOrDefault("avatarUrl", null);

            if (groupName == null || groupName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "groupName required"));
            }

            Group group = groupService.createGroup(groupName, description, userId, avatarUrl);
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            logger.error("Error creating group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all groups of current user
     */
    @GetMapping("/groups")
    public ResponseEntity<?> getUserGroups() {
        try {
            Integer userId = requireUserId();
            List<Group> groups = groupService.getUserGroups(userId);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            logger.error("Error getting user groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get group info
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getGroup(@PathVariable Integer groupId) {
        try {
            Group group = groupService.getGroup(groupId);
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            logger.error("Error getting group", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Group not found"));
        }
    }

    /**
     * Add member to group
     */
    @PostMapping("/group/{groupId}/members")
    public ResponseEntity<?> addMember(
            @PathVariable Integer groupId,
            @RequestBody Map<String, Integer> request) {
        try {
            Integer userId = requireUserId();
            Integer memberId = request.get("userId");

            // Check if user is admin
            if (!groupService.isAdmin(groupId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only admin can add members"));
            }

            GroupMember member = groupService.addMember(groupId, memberId);
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            logger.error("Error adding member", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove member from group
     */
    @DeleteMapping("/group/{groupId}/members/{memberId}")
    public ResponseEntity<?> removeMember(
            @PathVariable Integer groupId,
            @PathVariable Integer memberId) {
        try {
            Integer userId = requireUserId();

            // Check if user is admin
            if (!groupService.isAdmin(groupId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only admin can remove members"));
            }

            groupService.removeMember(groupId, memberId);
            return ResponseEntity.ok(Map.of("message", "Member removed"));
        } catch (Exception e) {
            logger.error("Error removing member", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get group members
     */
    @GetMapping("/group/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Integer groupId) {
        try {
            Integer userId = requireUserId();
            
            // Check if user is member
            if (!groupService.isMember(groupId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User is not member of this group"));
            }

            List<GroupMember> members = groupService.getGroupMembers(groupId);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            logger.error("Error getting group members", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Send message to group
     */
    @PostMapping("/group/{groupId}/send")
    public ResponseEntity<?> sendGroupMessage(
            @PathVariable Integer groupId,
            @RequestBody Map<String, Object> request) {
        try {
            Integer userId = requireUserId();
            
            // Check if user is member
            if (!groupService.isMember(groupId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User is not member of this group"));
            }

            String content = (String) request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "content required"));
            }

            User sender = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Group group = groupService.getGroup(groupId);

            Message message = new Message();
            message.setSender(sender);
            message.setGroup(group);
            message.setContent(content);
            message.setStatus(false); // Group messages start as unread

            Message savedMessage = messageRepository.save(message);

            // Increment unread count for all group members except sender
            groupService.incrementUnreadCount(groupId, userId);

            // Broadcast to all group members
            Map<String, Object> wsEvent = new HashMap<>();
            wsEvent.put("type", "GROUP_MESSAGE");
            wsEvent.put("groupId", groupId);
            wsEvent.put("data", messageService.toMessageResponse(savedMessage));
            chatWebSocketHandler.broadcastEvent(wsEvent);

            // Broadcast unread counts to all group members
            groupService.broadcastGroupUnreadCounts(groupId, chatWebSocketHandler);

            return ResponseEntity.ok(messageService.toMessageResponse(savedMessage));
        } catch (Exception e) {
            logger.error("Error sending group message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get group messages
     */
    @GetMapping("/group/{groupId}/messages")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getGroupMessages(
            @PathVariable Integer groupId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        try {
            Integer userId = requireUserId();
            
            // Check if user is member
            if (!groupService.isMember(groupId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User is not member of this group"));
            }

            // Get messages for this group
            List<Message> messages = messageRepository.findAll()
                    .stream()
                    .filter(m -> m.getGroup() != null && m.getGroup().getGroupId().equals(groupId))
                    .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
                    .skip((long) page * pageSize)
                    .limit(pageSize)
                    .toList();

            // Initialize lazy-loaded properties for each message
            for (Message m : messages) {
                if (m.getGroup() != null) {
                    org.hibernate.Hibernate.initialize(m.getGroup());
                }
                if (m.getSender() != null) {
                    org.hibernate.Hibernate.initialize(m.getSender());
                    org.hibernate.Hibernate.initialize(m.getSender().getRole());
                }
            }

            return ResponseEntity.ok(messages.stream()
                    .map(messageService::toMessageResponse)
                    .toList());
        } catch (Exception e) {
            logger.error("Error getting group messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update group info
     */
    @PutMapping("/group/{groupId}")
    public ResponseEntity<?> updateGroup(
            @PathVariable Integer groupId,
            @RequestBody Map<String, Object> request) {
        try {
            Integer userId = requireUserId();

            // Check if user is admin
            if (!groupService.isAdmin(groupId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only admin can update group"));
            }

            String groupName = (String) request.get("groupName");
            String description = (String) request.get("description");
            String avatarUrl = (String) request.get("avatarUrl");

            Group group = groupService.updateGroup(groupId, groupName, description, avatarUrl);
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            logger.error("Error updating group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete group
     */
    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Integer groupId) {
        try {
            Integer userId = requireUserId();

            // Check if user is admin
            if (!groupService.isAdmin(groupId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only admin can delete group"));
            }

            groupService.deleteGroup(groupId);
            return ResponseEntity.ok(Map.of("message", "Group deleted"));
        } catch (Exception e) {
            logger.error("Error deleting group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reset unread count for current user in group
     */
    @PostMapping("/group/{groupId}/mark-read")
    public ResponseEntity<?> markGroupAsRead(@PathVariable Integer groupId) {
        try {
            Integer userId = requireUserId();
            // Only reset unread count, don't mark individual messages as read
            // Users will see unread messages with special styling
            groupService.resetUnreadCount(groupId, userId);
            
            return ResponseEntity.ok(Map.of("message", "Marked as read"));
        } catch (Exception e) {
            logger.error("Error marking group as read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mark individual group message as read
     */
    @PostMapping("/group/message/{messageId}/mark-read")
    public ResponseEntity<?> markGroupMessageAsRead(@PathVariable Integer messageId) {
        try {
            Integer userId = requireUserId();
            
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message not found"));
            
            // Only allow marking as read if the reader is NOT the sender
            if (message.getSender().getUserId().equals(userId)) {
                // Sender cannot mark their own message as read
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot mark your own message as read"));
            }
            
            // Mark as read
            message.setStatus(true);
            message.setReadAt(java.time.LocalDateTime.now());
            Message updatedMessage = messageRepository.save(message);
            
            // Broadcast update to all group members via WebSocket
            Integer groupId = message.getGroup().getGroupId();
            Map<String, Object> wsEvent = new HashMap<>();
            wsEvent.put("type", "GROUP_MESSAGE_READ");
            wsEvent.put("messageId", messageId);
            wsEvent.put("readAt", updatedMessage.getReadAt());
            wsEvent.put("groupId", groupId);
            wsEvent.put("senderId", message.getSender().getUserId());
            chatWebSocketHandler.broadcastEvent(wsEvent);
            
            return ResponseEntity.ok(Map.of("message", "Message marked as read"));
        } catch (Exception e) {
            logger.error("Error marking group message as read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
