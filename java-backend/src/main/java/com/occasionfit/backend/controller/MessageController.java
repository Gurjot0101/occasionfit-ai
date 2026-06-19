package com.occasionfit.backend.controller;

import com.occasionfit.backend.dto.req.MessageRequest;
import com.occasionfit.backend.dto.res.MessageResponse;
import com.occasionfit.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Log4j2
public class MessageController {

    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody MessageRequest messageRequest, @AuthenticationPrincipal String email) {
        try {
            log.info(messageRequest);
            MessageResponse res = chatService.handleMessage(messageRequest, email);
            log.info(res);
            return ResponseEntity.status(HttpStatus.OK).body(res);
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send message");
        }
    }

    @GetMapping("/threads")
    public ResponseEntity<?> getThreads(@AuthenticationPrincipal String email) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(chatService.getChatThreads(email));
        } catch (Exception e) {
            log.error("Error getting threads: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get threads");
        }
    }

    @GetMapping("/messages/{threadId}")
    public ResponseEntity<?> getMessages(@PathVariable String threadId) {
        try {
            return ResponseEntity.ok(chatService.getMessages(threadId));
        } catch (Exception e) {
            log.error("Error getting messages for threadId:{} : {}", threadId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get messages");
        }
    }

    @DeleteMapping("/thread/{threadId}")
    public ResponseEntity<?> deleteThread(@PathVariable String threadId) {
        try {
            log.info("Deleting thread: {}", threadId);
            chatService.deleteChatThread(threadId);
            chatService.deleteMessages(threadId);
            return ResponseEntity.ok(Map.of("message", "Thread deleted"));
        } catch (Exception e) {
            log.error("Error deleting thread: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete thread");
        }
    }
}
