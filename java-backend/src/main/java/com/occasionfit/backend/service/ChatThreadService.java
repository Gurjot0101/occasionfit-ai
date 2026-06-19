package com.occasionfit.backend.service;

import com.occasionfit.backend.model.ChatThread;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ChatThreadService {

    ChatThread createChatThread(String email, String threadContext);

    ChatThread getThread(String threadId);

    List<ChatThread> getThreads(String email);

    void deleteThread(String threadId);
}
