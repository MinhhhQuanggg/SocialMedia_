package com.socialmedia.backend.controller;

import com.socialmedia.backend.entity.Message;
import com.socialmedia.backend.service.MessageService;
import com.socialmedia.backend.websocket.ChatWebSocketHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public MessageController(MessageService messageService, ChatWebSocketHandler chatWebSocketHandler) {
        this.messageService = messageService;
        this.chatWebSocketHandler = chatWebSocketHandler;
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

    // Lịch sử chat với 1 người
    @GetMapping("/with/{otherId}")
    public ResponseEntity<?> conversation(@PathVariable Integer otherId) {
        Integer me = requireUserId();
        List<Message> list = messageService.getConversation(me, otherId);
        return ResponseEntity.ok(list.stream().map(messageService::toMessageResponse).toList());
    }

    // Gửi tin qua REST (để test/backup). Realtime sẽ gửi qua WS.
    @PostMapping("/to/{otherId}")
    public ResponseEntity<?> sendRest(@PathVariable Integer otherId, @RequestBody Map<String, String> body) {
        Integer me = requireUserId();
        Message m = messageService.send(me, otherId, body);
        return ResponseEntity.ok(messageService.toMessageResponse(m));
    }

    // Upload media (images/videos) - fallback from WebSocket
    @PostMapping("/send-media")
    public ResponseEntity<?> sendMedia(
            @RequestParam Integer toUserId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String mediaType) {
        Integer me = requireUserId();
        
        try {
            Message m = messageService.sendMedia(me, toUserId, file, mediaType);
            
            // Broadcast via WebSocket to both users
            Map<String, Object> wsEvent = new HashMap<>();
            wsEvent.put("type", "NEW_MESSAGE");
            wsEvent.put("data", messageService.toMessageResponse(m));
            chatWebSocketHandler.broadcastEvent(wsEvent);
            
            return ResponseEntity.ok(messageService.toMessageResponse(m));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Mark read
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable("id") Integer messageId) {
        Integer me = requireUserId();
        Message m = messageService.markRead(me, messageId);
        return ResponseEntity.ok(messageService.toMessageResponse(m));
    }

    // Đếm unread (phục vụ badge notification)
    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount() {
        Integer me = requireUserId();
        return ResponseEntity.ok(Map.of("unread", messageService.countUnread(me)));
    }

    // Mark all messages from a user as read
    @PutMapping("/mark-read-from/{otherUserId}")
    public ResponseEntity<?> markAllReadFromUser(@PathVariable Integer otherUserId) {
        Integer me = requireUserId();
        int count = messageService.markAllReadFromUser(me, otherUserId);
        return ResponseEntity.ok(Map.of("marked", count));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        if ("UNAUTHORIZED".equals(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "UNAUTHORIZED"));
        }
        throw ex;
    }
}
