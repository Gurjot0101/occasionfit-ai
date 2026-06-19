package com.occasionfit.backend.service.impl;

import com.occasionfit.backend.model.ChatThread;
import com.occasionfit.backend.model.User;
import com.occasionfit.backend.repository.ChatThreadRepository;
import com.occasionfit.backend.repository.UserRepository;
import com.occasionfit.backend.service.ChatThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ChatThreadServiceImpl implements ChatThreadService {

    private final UserRepository userRepository;
    private final ChatThreadRepository chatThreadRepository;

    public ChatThread createChatThread(String email, String threadContext) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        ChatThread chatThread = ChatThread.builder()
                .userId(user.getId())
                .threadContext(threadContext != null ? threadContext : "")
                .messageCount(0)
                .totalTokens(0)
                .lastSummaryAtCount(0)
                .summaryTokenCount(0)
                .build();

        log.info("New thread created for user: {}", email);
        return chatThreadRepository.save(chatThread);
    }

    public ChatThread getThread(String threadId) {
        return chatThreadRepository.findById(threadId).orElseThrow(() -> new RuntimeException("Thread not found"));
    }

    public List<ChatThread> getThreads(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        log.info("Getting threads for user: {}", email);
        return chatThreadRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    public void deleteThread(String threadId) {
        chatThreadRepository.deleteById(threadId);
        log.info("Thread deleted: {}", threadId);
    }
}
